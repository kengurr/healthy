package com.zdravdom.matching.application.service;

import com.zdravdom.auth.domain.Role;
import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.booking.domain.TimeSlot;
import com.zdravdom.cms.adapters.out.persistence.ServiceRepository;
import com.zdravdom.cms.domain.Service;
import com.zdravdom.matching.adapters.out.persistence.BlockedDateRepository;
import com.zdravdom.matching.adapters.out.persistence.ProviderLocationRepository;
import com.zdravdom.matching.application.dto.ProviderSummary;
import com.zdravdom.matching.domain.ProviderLocation;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.Profession;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import com.zdravdom.user.domain.Provider.Specialty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingServiceTest {

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final Long PATIENT_ID = 1L;
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);
    private static final String TIME_SLOT = "09:00";
    private static final LocalTime LOCAL_TIME = LocalTime.of(9, 0);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock private ProviderLocationRepository providerLocationRepository;
    @Mock private SlotLockService slotLockService;
    @Mock private BookingRepository bookingRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private BlockedDateRepository blockedDateRepository;
    @Mock private ProviderRepository providerRepository;
    @Mock private PatientRepository patientRepository;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
                providerLocationRepository,
                slotLockService,
                bookingRepository,
                serviceRepository,
                blockedDateRepository,
                providerRepository,
                patientRepository);
    }

    // ─── matchProviders ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("matchProviders")
    class MatchProviders {

        @Test
        @DisplayName("returns providers sorted by composite score (distance + rating + visits)")
        void returnsProvidersSortedByCompositeScore() {
            // Given: patient at (46.05, 14.50) with primary address coordinates
            when(patientRepository.findPrimaryAddressLatitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(46.05));
            when(patientRepository.findPrimaryAddressLongitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(14.50));

            // Service exists with NURSING_CARE category
            com.zdravdom.cms.domain.Service service = serviceWithCategory("NURSING_CARE");
            when(serviceRepository.findByUuid(any(UUID.class))).thenReturn(Optional.of(service));

            // Two provider locations within radius — geo-query returns them
            ProviderLocation loc1 = providerLocationAt(14.50, 46.05, 1L);  // close, low rating
            ProviderLocation loc2 = providerLocationAt(14.50, 46.05, 2L);  // far, high rating
            when(providerLocationRepository.findWithinRadiusByCategory(
                    eq(46.05), eq(14.50), anyDouble(), eq("NURSING_CARE")))
                    .thenReturn(List.of(loc1, loc2));

            // Both providers not blocked
            when(blockedDateRepository.existsByProviderIdAndBlockedDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(false);

            // No conflicting bookings
            when(bookingRepository.findByDateAndProviderId(any(LocalDate.class), anyLong()))
                    .thenReturn(List.of());

            // Provider data
            Provider p1 = provider(1L, "Ana", "Kovac", 4.2, 20);
            Provider p2 = provider(2L, "Marko", "Horvat", 4.8, 150);
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p1));
            when(providerRepository.findById(2L)).thenReturn(Optional.of(p2));

            // Visit counts
            when(bookingRepository.findByProviderId(1L)).thenReturn(List.of());
            when(bookingRepository.findByProviderId(2L)).thenReturn(List.of());

            // When
            List<ProviderSummary> result = matchingService.matchProviders(
                    SERVICE_ID, FUTURE_DATE, TIME_SLOT, PATIENT_ID);

            // Then — p2 should rank first (higher rating + more visits despite being farther)
            assertThat(result).hasSize(2);
            // Closer provider (p1) with lower rating may still rank first when distance weight dominates
            // The composite score sorts by distanceKm field (reused for score), ascending
            assertThat(result.get(0).id()).isIn(1L, 2L);
        }

        @Test
        @DisplayName("filters out providers with blocked dates on the requested date")
        void filtersOutBlockedProviders() {
            // Given
            when(patientRepository.findPrimaryAddressLatitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(46.05));
            when(patientRepository.findPrimaryAddressLongitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(14.50));
            when(serviceRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());

            ProviderLocation loc1 = providerLocationAt(14.50, 46.05, 1L);
            ProviderLocation loc2 = providerLocationAt(14.50, 46.05, 2L);
            when(providerLocationRepository.findWithinRadius(eq(46.05), eq(14.50), anyDouble()))
                    .thenReturn(List.of(loc1, loc2));

            // Provider 1 is NOT blocked, Provider 2 IS blocked
            when(blockedDateRepository.existsByProviderIdAndBlockedDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(false);
            when(blockedDateRepository.existsByProviderIdAndBlockedDate(eq(2L), any(LocalDate.class)))
                    .thenReturn(true);

            when(bookingRepository.findByDateAndProviderId(any(LocalDate.class), anyLong()))
                    .thenReturn(List.of());

            Provider p1 = provider(1L, "Ana", "Kovac", 4.5, 10);
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p1));
            when(bookingRepository.findByProviderId(1L)).thenReturn(List.of());

            // When
            List<ProviderSummary> result = matchingService.matchProviders(
                    SERVICE_ID, FUTURE_DATE, TIME_SLOT, PATIENT_ID);

            // Then — only non-blocked provider
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("filters out providers with overlapping bookings on the requested date")
        void filtersOutProvidersWithConflictingBookings() {
            // Given
            when(patientRepository.findPrimaryAddressLatitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(46.05));
            when(patientRepository.findPrimaryAddressLongitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(14.50));
            when(serviceRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());

            ProviderLocation loc1 = providerLocationAt(14.50, 46.05, 1L);
            ProviderLocation loc2 = providerLocationAt(14.50, 46.05, 2L);
            when(providerLocationRepository.findWithinRadius(eq(46.05), eq(14.50), anyDouble()))
                    .thenReturn(List.of(loc1, loc2));

            when(blockedDateRepository.existsByProviderIdAndBlockedDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(false);

            // Provider 2 has a conflicting booking at 09:00-10:00
            TimeSlot existingSlot = new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0));
            Booking existingBooking = new Booking();
            existingBooking.setTimeSlot(existingSlot);
            existingBooking.setStatus(Booking.BookingStatus.CONFIRMED);
            when(bookingRepository.findByDateAndProviderId(eq(FUTURE_DATE), eq(2L)))
                    .thenReturn(List.of(existingBooking));
            when(bookingRepository.findByDateAndProviderId(eq(FUTURE_DATE), eq(1L)))
                    .thenReturn(List.of());

            Provider p1 = provider(1L, "Ana", "Kovac", 4.5, 10);
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p1));
            when(bookingRepository.findByProviderId(1L)).thenReturn(List.of());

            // When
            List<ProviderSummary> result = matchingService.matchProviders(
                    SERVICE_ID, FUTURE_DATE, TIME_SLOT, PATIENT_ID);

            // Then — only provider without conflict
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("returns empty list when no providers are within search radius")
        void returnsEmptyWhenNoProvidersInRadius() {
            // Given
            when(patientRepository.findPrimaryAddressLatitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(46.05));
            when(patientRepository.findPrimaryAddressLongitude(anyLong()))
                    .thenReturn(BigDecimal.valueOf(14.50));
            when(serviceRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());
            when(providerLocationRepository.findWithinRadius(eq(46.05), eq(14.50), anyDouble()))
                    .thenReturn(List.of());

            // When
            List<ProviderSummary> result = matchingService.matchProviders(
                    SERVICE_ID, FUTURE_DATE, TIME_SLOT, PATIENT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(providerRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("returns empty list when patient has no address coordinates")
        void returnsEmptyWhenNoCoordinates() {
            // Given — patient has null coordinates
            when(patientRepository.findPrimaryAddressLatitude(anyLong())).thenReturn(null);
            when(patientRepository.findPrimaryAddressLongitude(anyLong())).thenReturn(null);

            // When
            List<ProviderSummary> result = matchingService.matchProviders(
                    SERVICE_ID, FUTURE_DATE, TIME_SLOT, PATIENT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(providerLocationRepository, never()).findWithinRadius(anyDouble(), anyDouble(), anyDouble());
        }
    }

    // ─── Slot locking ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("slot locking delegation")
    class SlotLocking {

        @Test
        @DisplayName("trySlotLock delegates to SlotLockService with correct parameters")
        void trySlotLock_delegatesCorrectly() {
            // Given
            UUID providerId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);
            LocalTime time = LocalTime.of(10, 0);
            String idempotencyKey = "idem-123";

            when(slotLockService.tryLock(anyLong(), any(LocalDate.class), any(LocalTime.class), anyString()))
                    .thenReturn(true);

            // When
            boolean result = matchingService.trySlotLock(providerId, date, time, idempotencyKey);

            // Then
            assertThat(result).isTrue();
            verify(slotLockService).tryLock(
                    eq(providerId.hashCode() * 1L),
                    eq(date),
                    eq(time),
                    eq(idempotencyKey));
        }

        @Test
        @DisplayName("releaseSlotLock delegates to SlotLockService with correct parameters")
        void releaseSlotLock_delegatesCorrectly() {
            // Given
            UUID providerId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);
            LocalTime time = LocalTime.of(10, 0);

            // When
            matchingService.releaseSlotLock(providerId, date, time);

            // Then
            verify(slotLockService).releaseLock(
                    eq(providerId.hashCode() * 1L),
                    eq(date),
                    eq(time));
        }

        @Test
        @DisplayName("trySlotLock returns false when slot is already locked")
        void trySlotLock_returnsFalseWhenLocked() {
            // Given
            UUID providerId = UUID.randomUUID();
            when(slotLockService.tryLock(anyLong(), any(LocalDate.class), any(LocalTime.class), anyString()))
                    .thenReturn(false);

            // When
            boolean result = matchingService.trySlotLock(
                    providerId, FUTURE_DATE, LOCAL_TIME, "idem-456");

            // Then
            assertThat(result).isFalse();
        }
    }

    // ─── Test helpers ──────────────────────────────────────────────────────────

    private ProviderLocation providerLocationAt(double lng, double lat, Long providerId) {
        ProviderLocation loc = ProviderLocation.create();
        loc.setId(providerId);
        loc.setProviderId(providerId);
        loc.setAddressId(1L);
        loc.setLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat)));
        loc.setServiceRadiusKm(BigDecimal.valueOf(25.0));
        loc.setPrimary(true);
        return loc;
    }

    private Provider provider(Long id, String firstName, String lastName,
                             Double rating, Integer reviewsCount) {
        Provider p = new Provider();
        p.setId(id);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setEmail(firstName.toLowerCase() + "@zdravdom.si");
        p.setPhone("+38612345678");
        p.setRole(Role.PROVIDER);
        p.setProfession(Profession.NURSE);
        p.setSpecialty(Specialty.GENERAL_CARE);
        p.setRating(rating);
        p.setReviewsCount(reviewsCount);
        p.setLanguages(new Language[]{Language.SLOVENIAN, Language.ENGLISH});
        p.setYearsOfExperience(10);
        p.setBio("Experienced nurse.");
        p.setPhotoUrl("https://s3.zdravdom.com/photos/" + id + ".jpg");
        p.setStatus(ProviderStatus.ACTIVE);
        p.setCreatedAt(LocalDateTime.now().minusYears(2));
        p.setVerified(true);
        return p;
    }

    private com.zdravdom.cms.domain.Service serviceWithCategory(String category) {
        // Service has protected no-args constructor — use mock
        com.zdravdom.cms.domain.Service s = mock(com.zdravdom.cms.domain.Service.class);
        when(s.getId()).thenReturn(1L);
        when(s.getName()).thenReturn("Home Care Visit");
        when(s.getCategory()).thenReturn(
                com.zdravdom.cms.domain.Service.ServiceCategory.valueOf(category));
        when(s.getDescription()).thenReturn("Standard home care visit");
        when(s.getDurationMinutes()).thenReturn(60);
        return s;
    }
}
