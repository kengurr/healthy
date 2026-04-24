package com.zdravdom.matching.application.service;

import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.booking.domain.TimeSlot;
import com.zdravdom.cms.adapters.out.persistence.ServiceRepository;
import com.zdravdom.matching.adapters.out.persistence.BlockedDateRepository;
import com.zdravdom.matching.adapters.out.persistence.ProviderLocationRepository;
import com.zdravdom.matching.application.dto.ProviderSummary;
import com.zdravdom.matching.domain.ProviderLocation;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.domain.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for provider matching based on service, location, and availability.
 *
 * Workflow:
 * 1. Resolve patient address coordinates from addressId
 * 2. Geo-query providers within service radius using PostGIS ST_DWithin
 * 3. Filter by service category (via ServiceRepository)
 * 4. Filter out providers with blocked_dates entries for the requested date
 * 5. Filter out providers with conflicting bookings (same date + overlapping time slot)
 * 6. Rank by composite score: distance (0.4) + rating (0.3) + visit count (0.3)
 * 7. Return ProviderSummary list sorted by composite score
 *
 * Slot locking uses Redis (Redisson RLock) with DB fallback via SlotLockService.
 */
@org.springframework.stereotype.Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private static final double DEFAULT_SEARCH_RADIUS_KM = 25.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    // Composite score weights
    private static final double DISTANCE_WEIGHT = 0.4;
    private static final double RATING_WEIGHT = 0.3;
    private static final double VISITS_WEIGHT = 0.3;

    private final ProviderLocationRepository providerLocationRepository;
    private final SlotLockService slotLockService;
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final BlockedDateRepository blockedDateRepository;
    private final ProviderRepository providerRepository;
    private final PatientRepository patientRepository;

    public MatchingService(
            ProviderLocationRepository providerLocationRepository,
            SlotLockService slotLockService,
            BookingRepository bookingRepository,
            ServiceRepository serviceRepository,
            BlockedDateRepository blockedDateRepository,
            ProviderRepository providerRepository,
            PatientRepository patientRepository) {
        this.providerLocationRepository = providerLocationRepository;
        this.slotLockService = slotLockService;
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.blockedDateRepository = blockedDateRepository;
        this.providerRepository = providerRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Match providers by service, date, time, and patient.
     * Returns providers sorted by composite match score (distance + rating + visits).
     */
    @Transactional(readOnly = true)
    public List<ProviderSummary> matchProviders(UUID serviceId, LocalDate date,
                                               String time, Long patientId) {
        log.info("Matching providers for service: {}, date: {}, time: {}, patientId: {}",
                serviceId, date, time, patientId);

        // 1. Resolve patient address coordinates from primary address
        BigDecimal lat = patientRepository.findPrimaryAddressLatitude(patientId);
        BigDecimal lng = patientRepository.findPrimaryAddressLongitude(patientId);
        if (lat == null || lng == null) {
            log.warn("Patient {} has no primary address coordinates, returning empty list", patientId);
            return List.of();
        }

        // 2. Get service to determine category filter
        com.zdravdom.cms.domain.Service service = serviceRepository.findByUuid(serviceId).orElse(null);
        String categoryFilter = (service != null) ? service.getCategory().name() : null;

        // 3. Geo-query providers within radius
        List<ProviderLocation> locations;
        if (categoryFilter != null) {
            locations = providerLocationRepository.findWithinRadiusByCategory(
                    lat.doubleValue(), lng.doubleValue(),
                    DEFAULT_SEARCH_RADIUS_KM * 1000.0, categoryFilter);
        } else {
            locations = providerLocationRepository.findWithinRadius(
                    lat.doubleValue(), lng.doubleValue(),
                    DEFAULT_SEARCH_RADIUS_KM * 1000.0);
        }

        if (locations.isEmpty()) {
            log.debug("No provider locations found within {} km of ({}, {})",
                    DEFAULT_SEARCH_RADIUS_KM, lat, lng);
            return List.of();
        }

        // 4. Parse requested time slot
        LocalTime requestedTime = parseTime(time);
        TimeSlot requestedSlot = new TimeSlot(requestedTime, requestedTime.plusHours(1));

        // 5. Build ProviderSummary list with scoring
        List<ProviderSummary> results = locations.stream()
                .map(loc -> buildProviderSummary(loc, lat, lng))
                .filter(summary -> summary != null)
                .filter(summary -> !isBlocked(summary.id(), date))
                .filter(summary -> !hasConflictingBooking(summary.id(), date, requestedSlot))
                .sorted(Comparator.comparingDouble(ProviderSummary::distanceKm))
                .toList();

        log.info("Matched {} providers for service {} at {} ({}, {})",
                results.size(), serviceId, date, lat, lng);
        return results;
    }

    /**
     * Try to acquire a slot lock for a provider/date/time.
     * Returns true if lock acquired, false if slot is already locked.
     *
     * @param providerId Long database ID of the provider (not a UUID — Provider has no uuid field)
     */
    public boolean trySlotLock(Long providerId, LocalDate date,
                                LocalTime timeSlot, String idempotencyKey) {
        log.info("Attempting to lock slot - providerId: {}, date: {}, time: {}",
                providerId, date, timeSlot);
        return slotLockService.tryLock(providerId, date, timeSlot, idempotencyKey);
    }

    /**
     * Release a slot lock.
     *
     * @param providerId Long database ID of the provider (not a UUID — Provider has no uuid field)
     */
    public void releaseSlotLock(Long providerId, LocalDate date, LocalTime timeSlot) {
        log.info("Releasing slot lock - providerId: {}, date: {}, time: {}",
                providerId, date, timeSlot);
        slotLockService.releaseLock(providerId, date, timeSlot);
    }

    /**
     * Build a ProviderSummary from a ProviderLocation.
     * Enriches with Provider data (rating, languages, bio, etc.) and
     * calculates distance from the search point.
     */
    private ProviderSummary buildProviderSummary(ProviderLocation loc,
                                                  BigDecimal searchLat, BigDecimal searchLng) {
        Provider provider = providerRepository.findById(loc.getProviderId()).orElse(null);
        if (provider == null) {
            return null;
        }

        double distanceKm = calculateDistance(
                searchLat.doubleValue(), searchLng.doubleValue(),
                extractLatitude(loc), extractLongitude(loc));

        int visitCount = (int) bookingRepository.countByProviderId(provider.getId());
        double compositeScore = calculateCompositeScore(distanceKm, provider.getRating(), visitCount);

        return new ProviderSummary(
                provider.getId(),
                provider.getFirstName(),
                provider.getLastName(),
                provider.getEmail(),
                provider.getPhone(),
                provider.getRole(),
                provider.getProfession(),
                provider.getSpecialty(),
                provider.getRating(),
                provider.getReviewsCount(),
                (provider.getLanguages() != null) ? List.of(provider.getLanguages()) : List.of(),
                provider.getYearsOfExperience(),
                provider.getBio(),
                provider.getPhotoUrl(),
                provider.getStatus(),
                compositeScore,
                provider.getCreatedAt()
        );
    }

    /**
     * Calculate composite match score for ranking.
     * Normalized: distance (0-1, lower is better) * 0.4 + rating (0-1) * 0.3 + visits (0-1) * 0.3
     */
    private double calculateCompositeScore(double distanceKm, Double rating, int visitCount) {
        // Normalize distance: cap at 25km → score 0..1 (closer = higher)
        double distanceScore = Math.max(0.0, 1.0 - (distanceKm / DEFAULT_SEARCH_RADIUS_KM));
        // Normalize rating: 0-5 → 0-1
        double ratingScore = (rating != null) ? rating / 5.0 : 0.0;
        // Normalize visits: log scale, cap at 200 visits → 0-1
        double visitScore = Math.min(1.0, Math.log1p(visitCount) / Math.log1p(200));

        return (DISTANCE_WEIGHT * distanceScore)
                + (RATING_WEIGHT * ratingScore)
                + (VISITS_WEIGHT * visitScore);
    }

    /**
     * Haversine distance between two lat/lng points in km.
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private boolean isBlocked(Long providerId, LocalDate date) {
        return blockedDateRepository.existsByProviderIdAndBlockedDate(providerId, date);
    }

    private boolean hasConflictingBooking(Long providerId, LocalDate date, TimeSlot requestedSlot) {
        List<Booking> bookings = bookingRepository.findByDateAndProviderId(date, providerId);
        return bookings.stream()
                .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                .anyMatch(b -> {
                    TimeSlot existing = b.getTimeSlot();
                    return existing != null && existing.overlaps(requestedSlot);
                });
    }

    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            log.warn("Could not parse time '{}', defaulting to 08:00", time);
            return LocalTime.of(8, 0);
        }
    }

    /**
     * Extract latitude from PostGIS GEOGRAPHY(POINT, 4326).
     * The geography column stores (lng, lat) in PostGIS, so Point.getY() = latitude.
     */
    private double extractLatitude(ProviderLocation loc) {
        if (loc.getLocation() != null) {
            return loc.getLocation().getY();
        }
        return 0.0;
    }

    /**
     * Extract longitude from PostGIS GEOGRAPHY(POINT, 4326).
     * The geography column stores (lng, lat) in PostGIS, so Point.getX() = longitude.
     */
    private double extractLongitude(ProviderLocation loc) {
        if (loc.getLocation() != null) {
            return loc.getLocation().getX();
        }
        return 0.0;
    }
}
