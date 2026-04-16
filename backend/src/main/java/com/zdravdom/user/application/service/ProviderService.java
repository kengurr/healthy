package com.zdravdom.user.application.service;

import com.zdravdom.user.application.dto.ProviderResponse;
import com.zdravdom.user.application.dto.ProviderAvailabilityResponse;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest.DayOfWeek;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest.WeeklyScheduleItem;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.Profession;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import com.zdravdom.user.domain.Provider.Specialty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for provider profile management.
 */
@Service
public class ProviderService {

    private static final Logger log = LoggerFactory.getLogger(ProviderService.class);

    @Transactional
    public ProviderResponse getProviderByUserId(UUID userId) {
        Provider provider = getOrCreateProvider(userId);
        return toResponse(provider);
    }

    @Transactional
    public ProviderAvailabilityResponse getAvailability(UUID userId) {
        Provider provider = getOrCreateProvider(userId);
        // In production, query provider availability settings
        return new ProviderAvailabilityResponse(
            List.of(
                new ProviderAvailabilityResponse.WeeklyScheduleItem(
                    DayOfWeek.MONDAY, "08:00", "17:00"
                ),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(
                    DayOfWeek.TUESDAY, "08:00", "17:00"
                ),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(
                    DayOfWeek.WEDNESDAY, "08:00", "17:00"
                ),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(
                    DayOfWeek.THURSDAY, "08:00", "17:00"
                ),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(
                    DayOfWeek.FRIDAY, "08:00", "15:00"
                )
            ),
            List.of()
        );
    }

    @Transactional
    public ProviderAvailabilityResponse updateAvailability(UUID userId, UpdateAvailabilityRequest request) {
        log.info("Updated availability for provider userId: {}", userId);
        return new ProviderAvailabilityResponse(
            request.weeklySchedule() != null ?
                request.weeklySchedule().stream()
                    .map(item -> new ProviderAvailabilityResponse.WeeklyScheduleItem(
                        item.day(), item.startTime(), item.endTime()))
                    .toList() : List.of(),
            request.blockedDates() != null ? request.blockedDates() : List.of()
        );
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(UUID userId, String documentType, byte[] fileData, String fileName) {
        // In production, upload to S3 and store document metadata
        Long docId = System.currentTimeMillis();
        log.info("Uploaded document {} for provider userId: {}", documentType, userId);
        return new DocumentUploadResponse(
            docId,
            "https://s3.zdravdom.com/documents/" + docId,
            documentType,
            "PENDING_REVIEW"
        );
    }

    public record DocumentUploadResponse(
        Long id,
        String url,
        String type,
        String status
    ) {}

    private Provider getOrCreateProvider(UUID userId) {
        // For MVP: create a mock provider. In production, query by userId
        return new Provider(
            1L,
            "Dr. Marko",
            "Horvat",
            "provider@example.com",
            "+38698765432",
            com.zdravdom.auth.domain.Role.PROVIDER,
            Profession.NURSE,
            Specialty.GENERAL_CARE,
            4.8,
            127,
            List.of(Language.SLOVENIAN, Language.ENGLISH),
            12,
            "Experienced home healthcare nurse specializing in elderly care and chronic disease management.",
            "https://s3.zdravdom.com/photos/provider-1.jpg",
            ProviderStatus.ACTIVE,
            LocalDateTime.now().minusYears(2),
            LocalDateTime.now(),
            true
        );
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
            provider.id(),
            provider.firstName(),
            provider.lastName(),
            provider.email(),
            provider.phone(),
            provider.role(),
            provider.profession(),
            provider.specialty(),
            provider.rating(),
            provider.reviewsCount(),
            provider.languages(),
            provider.yearsOfExperience(),
            provider.bio(),
            provider.photoUrl(),
            provider.status(),
            provider.createdAt()
        );
    }
}