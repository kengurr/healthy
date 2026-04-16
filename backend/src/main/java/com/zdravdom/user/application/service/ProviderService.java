package com.zdravdom.user.application.service;

import com.zdravdom.user.application.dto.ProviderResponse;
import com.zdravdom.user.application.dto.ProviderAvailabilityResponse;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest.DayOfWeek;
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
import java.util.Arrays;
import java.util.List;

/**
 * Service for provider profile management.
 */
@Service
public class ProviderService {

    private static final Logger log = LoggerFactory.getLogger(ProviderService.class);

    @Transactional
    public ProviderResponse getProviderByUserId(Long userId) {
        Provider provider = getOrCreateProvider(userId);
        return toResponse(provider);
    }

    @Transactional
    public ProviderAvailabilityResponse getAvailability(Long userId) {
        Provider provider = getOrCreateProvider(userId);
        return new ProviderAvailabilityResponse(
            List.of(
                new ProviderAvailabilityResponse.WeeklyScheduleItem(DayOfWeek.MONDAY, "08:00", "17:00"),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(DayOfWeek.TUESDAY, "08:00", "17:00"),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(DayOfWeek.WEDNESDAY, "08:00", "17:00"),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(DayOfWeek.THURSDAY, "08:00", "17:00"),
                new ProviderAvailabilityResponse.WeeklyScheduleItem(DayOfWeek.FRIDAY, "08:00", "15:00")
            ),
            List.of()
        );
    }

    @Transactional
    public ProviderAvailabilityResponse updateAvailability(Long userId, UpdateAvailabilityRequest request) {
        log.info("Updated availability for provider userId: {}", userId);
        return new ProviderAvailabilityResponse(
            request.weeklySchedule() != null ?
                request.weeklySchedule().stream()
                    .map(item -> new ProviderAvailabilityResponse.WeeklyScheduleItem(item.day(), item.startTime(), item.endTime()))
                    .toList() : List.of(),
            request.blockedDates() != null ? request.blockedDates() : List.of()
        );
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(Long userId, String documentType, byte[] fileData, String fileName) {
        Long docId = System.currentTimeMillis();
        log.info("Uploaded document {} for provider userId: {}", documentType, userId);
        return new DocumentUploadResponse(
            docId,
            "https://s3.zdravdom.com/documents/" + docId,
            documentType,
            "PENDING_REVIEW"
        );
    }

    public record DocumentUploadResponse(Long id, String url, String type, String status) {}

    private Provider getOrCreateProvider(Long userId) {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setFirstName("Dr. Marko");
        provider.setLastName("Horvat");
        provider.setEmail("provider@example.com");
        provider.setPhone("+38698765432");
        provider.setRole(Role.PROVIDER);
        provider.setProfession(Profession.NURSE);
        provider.setSpecialty(Specialty.GENERAL_CARE);
        provider.setRating(4.8);
        provider.setReviewsCount(127);
        provider.setLanguages(new Language[]{Language.SLOVENIAN, Language.ENGLISH});
        provider.setYearsOfExperience(12);
        provider.setBio("Experienced home healthcare nurse specializing in elderly care and chronic disease management.");
        provider.setPhotoUrl("https://s3.zdravdom.com/photos/provider-1.jpg");
        provider.setStatus(ProviderStatus.ACTIVE);
        provider.setCreatedAt(LocalDateTime.now().minusYears(2));
        provider.setUpdatedAt(LocalDateTime.now());
        provider.setVerified(true);
        return provider;
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
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
            provider.getLanguages() != null ? Arrays.asList(provider.getLanguages()) : List.of(),
            provider.getYearsOfExperience(),
            provider.getBio(),
            provider.getPhotoUrl(),
            provider.getStatus(),
            provider.getCreatedAt()
        );
    }
}
