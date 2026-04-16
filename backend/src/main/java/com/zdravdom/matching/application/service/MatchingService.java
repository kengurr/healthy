package com.zdravdom.matching.application.service;

import com.zdravdom.matching.application.dto.ProviderSummary;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.Profession;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import com.zdravdom.user.domain.Provider.Specialty;
import com.zdravdom.auth.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for provider matching based on service, location, and availability.
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    /**
     * Match providers by service, date, time, and address.
     * Returns providers sorted by distance and rating.
     */
    @Transactional(readOnly = true)
    public List<ProviderSummary> matchProviders(UUID serviceId, java.time.LocalDate date,
                                                 String time, UUID addressId) {
        log.info("Matching providers for service: {}, date: {}, time: {}, address: {}",
            serviceId, date, time, addressId);

        // In production, this would:
        // 1. Get address coordinates from addressId
        // 2. Query providers by service category
        // 3. Filter by availability (weekly schedule + blocked dates)
        // 4. Calculate distance using geo-query (PostGIS)
        // 5. Sort by distance/rating and return

        List<ProviderSummary> providers = List.of(
            new ProviderSummary(
                1L, "Dr. Marko", "Horvat", "mhorvat@zdravdom.si", "+38612345678",
                Role.PROVIDER, Profession.NURSE, Specialty.GENERAL_CARE,
                4.8, 127, List.of(Language.SLOVENIAN, Language.ENGLISH),
                12, "Experienced home healthcare nurse specializing in elderly care.",
                "https://s3.zdravdom.com/photos/provider-1.jpg",
                ProviderStatus.ACTIVE, 2.3, LocalDateTime.now().minusYears(2)
            ),
            new ProviderSummary(
                2L, "Ana", "Kovač", "akovac@zdravdom.si", "+38698765432",
                Role.PROVIDER, Profession.PHYSIOTHERAPIST, Specialty.REHABILITATION,
                4.9, 85, List.of(Language.SLOVENIAN, Language.GERMAN),
                8, "Specialized in post-surgery rehabilitation and chronic pain management.",
                "https://s3.zdravdom.com/photos/provider-2.jpg",
                ProviderStatus.ACTIVE, 4.1, LocalDateTime.now().minusYears(3)
            ),
            new ProviderSummary(
                3L, "Peter", "Zupan", "pzupan@zdravdom.si", "+38651234567",
                Role.PROVIDER, Profession.NURSE, Specialty.WOUND_CARE,
                4.7, 64, List.of(Language.SLOVENIAN, Language.ENGLISH, Language.ITALIAN),
                15, "Expert wound care specialist with extensive experience in chronic wound management.",
                "https://s3.zdravdom.com/photos/provider-3.jpg",
                ProviderStatus.ACTIVE, 5.8, LocalDateTime.now().minusYears(5)
            )
        );

        // Sort by distance (mock sorting - in production would use actual geo-query)
        return providers.stream()
            .sorted(Comparator.comparing(ProviderSummary::distanceKm))
            .toList();
    }

    /**
     * Try to acquire a slot lock for a provider/date/time.
     * Returns true if lock acquired, false if slot is already locked.
     */
    public boolean trySlotLock(UUID providerId, java.time.LocalDate date,
                               java.time.LocalTime timeSlot, String idempotencyKey) {
        log.info("Attempting to lock slot - provider: {}, date: {}, time: {}",
            providerId, date, timeSlot);

        // In production, use Redis SETNX with TTL
        // String lockKey = "slot:" + providerId + ":" + date + ":" + timeSlot;
        // Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, idempotencyKey, Duration.ofMinutes(5));

        return true; // Mock always succeeds
    }

    /**
     * Release a slot lock.
     */
    public void releaseSlotLock(UUID providerId, java.time.LocalDate date,
                               java.time.LocalTime timeSlot) {
        log.info("Releasing slot lock - provider: {}, date: {}, time: {}",
            providerId, date, timeSlot);
        // In production, delete Redis key
    }
}