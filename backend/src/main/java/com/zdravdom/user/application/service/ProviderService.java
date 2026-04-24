package com.zdravdom.user.application.service;

import com.zdravdom.auth.domain.Role;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderScheduleRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderDocumentRepository;
import com.zdravdom.user.application.dto.ProviderAvailabilityResponse;
import com.zdravdom.user.application.dto.ProviderResponse;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest;
import com.zdravdom.user.application.mapper.ProviderMapper;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import com.zdravdom.user.domain.Provider.Specialty;
import com.zdravdom.user.domain.ProviderSchedule;
import com.zdravdom.user.domain.ProviderSchedule.DayOfWeek;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service for provider profile management.
 * All operations are backed by the real database with optimistic locking
 * (@Version) and dynamic SQL updates (@DynamicUpdate).
 */
@Service
public class ProviderService {

    private static final Logger log = LoggerFactory.getLogger(ProviderService.class);

    private final ProviderRepository providerRepository;
    private final ProviderScheduleRepository scheduleRepository;
    private final ProviderDocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public ProviderService(
            ProviderRepository providerRepository,
            ProviderScheduleRepository scheduleRepository,
            ProviderDocumentRepository documentRepository,
            DocumentStorageService documentStorageService) {
        this.providerRepository = providerRepository;
        this.scheduleRepository = scheduleRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
    }

    @Transactional(readOnly = true)
    public ProviderResponse getProviderByUserId(Long userId) {
        Provider provider = providerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", userId));
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public ProviderAvailabilityResponse getAvailability(Long userId) {
        Provider provider = providerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", userId));

        List<ProviderSchedule> schedules = scheduleRepository.findByProviderId(provider.getId());

        List<ProviderAvailabilityResponse.WeeklyScheduleItem> weeklySlots = schedules.stream()
            .filter(s -> !s.isBlocked() && s.getDayOfWeek() != null)
            .map(s -> {
                UpdateAvailabilityRequest.DayOfWeek day = UpdateAvailabilityRequest.DayOfWeek.valueOf(s.getDayOfWeek().name());
                return new ProviderAvailabilityResponse.WeeklyScheduleItem(
                    day, s.getStartTime().toString(), s.getEndTime().toString());
            })
            .toList();

        List<java.time.LocalDate> blockedDates = schedules.stream()
            .filter(ProviderSchedule::isBlocked)
            .map(ProviderSchedule::getBlockedDate)
            .filter(d -> d != null)
            .toList();

        return new ProviderAvailabilityResponse(weeklySlots, blockedDates);
    }

    @Transactional
    public ProviderAvailabilityResponse updateAvailability(Long userId, UpdateAvailabilityRequest request) {
        Provider provider = providerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", userId));

        // Replace all existing non-blocked slots with the new schedule
        scheduleRepository.deleteByProviderIdAndIsBlockedFalse(provider.getId());

        if (request.weeklySchedule() != null) {
            for (UpdateAvailabilityRequest.WeeklyScheduleItem item : request.weeklySchedule()) {
                DayOfWeek day = DayOfWeek.valueOf(item.day().name());
                LocalTime start = LocalTime.parse(item.startTime());
                LocalTime end = LocalTime.parse(item.endTime());

                ProviderSchedule slot = ProviderSchedule.availableSlot(provider.getId(), day, start, end);
                scheduleRepository.save(slot);
            }
        }

        if (request.blockedDates() != null) {
            for (LocalDate date : request.blockedDates()) {
                // Check if blocked date already exists
                if (scheduleRepository.findByProviderIdAndBlockedDate(provider.getId(), date).isEmpty()) {
                    scheduleRepository.save(ProviderSchedule.blocked(provider.getId(), date));
                }
            }
        }

        log.info("Updated availability for provider userId: {}", userId);
        return getAvailability(userId);
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(Long userId, String documentType, byte[] fileData, String fileName) {
        Provider provider = providerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", userId));

        String s3Key;
        try {
            s3Key = documentStorageService.uploadDocument(
                new ByteArrayMultipartFile(fileData, fileName),
                provider.getId(),
                documentType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document to S3", e);
        }

        // TODO (Production): Persist ProviderDocument entity to get the real database ID.
        // Currently returns s3Key as the document reference — replace 0L with:
        //   ProviderDocument doc = new ProviderDocument(provider.getId(), s3Key, documentType, fileName, "PENDING_REVIEW");
        //   ProviderDocument saved = documentRepository.save(doc);
        //   return new DocumentUploadResponse(saved.getId(), ..., "PENDING_REVIEW");
        log.info("Uploaded document {} for provider userId: {}", documentType, userId);

        // TODO (Production): Replace hardcoded S3 domain with configurable cloudfront-distribution domain:
        //   s3Key is the safe reference — construct URL from ${app.document.base-url} property
        //   (supports CloudFront CDN, MinIO for local dev, or any S3-compatible endpoint)
        // DEVELOPMENT: Stub returns hardcoded URL and id=0L — production needs real S3 + ProviderDocument entity (see TODOs above)
        String documentUrl = "https://s3.amazonaws.com/zdravdom-documents/" + s3Key;
        return new DocumentUploadResponse(0L, documentUrl, documentType, "PENDING_REVIEW");
    }

    @Transactional(readOnly = true)
    public ProviderResponse getProviderById(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));
        return toResponse(provider);
    }

    public record DocumentUploadResponse(Long id, String url, String type, String status) {}

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

    // Minimal MultipartFile replacement for byte[] input
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        private final String contentType;

        ByteArrayMultipartFile(byte[] content, String filename) {
            this.content = content;
            this.filename = filename;
            this.contentType = "application/octet-stream";
        }

        @Override public String getName() { return filename; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }
        @Override public void transferTo(java.io.File dest) throws IOException {
            try (var out = new java.io.FileOutputStream(dest)) {
                out.write(content);
            }
        }
    }

    private record ProviderDocumentResponse(Long id, String s3Key, String documentType, String fileName, String status) {}
}
