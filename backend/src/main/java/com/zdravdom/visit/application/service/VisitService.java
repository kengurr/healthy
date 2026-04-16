package com.zdravdom.visit.application.service;

import com.zdravdom.visit.application.dto.*;
import com.zdravdom.visit.application.dto.CompleteVisitRequest.VitalsRequest;
import com.zdravdom.visit.domain.Visit;
import com.zdravdom.visit.domain.Visit.VisitStatus;
import com.zdravdom.visit.domain.Vitals;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for visit management.
 */
@Service
public class VisitService {

    private static final Logger log = LoggerFactory.getLogger(VisitService.class);

    @Transactional(readOnly = true)
    public VisitResponse getVisitById(UUID visitId) {
        Visit visit = getOrCreateMockVisit(toLong(visitId));
        return toResponse(visit);
    }

    @Transactional
    public VisitResponse startVisit(UUID visitId, UUID providerId) {
        Visit visit = getOrCreateMockVisit(toLong(visitId));

        if (!visit.isInProgress()) {
            throw new ValidationException("Visit cannot be started in current status");
        }

        Visit started = new Visit(
            visit.id(), visit.bookingId(), toLong(providerId), visit.patientId(),
            visit.vitals(), visit.clinicalNotes(), visit.proceduresPerformed(),
            visit.photos(), visit.recommendations(), visit.patientSignature(),
            VisitStatus.IN_PROGRESS, visit.reportUrl(),
            LocalDateTime.now(), visit.completedAt(),
            visit.createdAt(), LocalDateTime.now()
        );

        log.info("Visit {} started by provider {}", visitId, providerId);
        return toResponse(started);
    }

    @Transactional
    public VisitResponse completeVisit(UUID visitId, CompleteVisitRequest request) {
        Visit visit = getOrCreateMockVisit(toLong(visitId));

        Vitals vitals = new Vitals(
            request.vitals().bloodPressure(),
            request.vitals().heartRate(),
            request.vitals().temperature(),
            request.vitals().o2Saturation(),
            request.vitals().respiratoryRate(),
            request.vitals().bloodGlucose(),
            request.vitals().weight(),
            request.vitals().notes(),
            LocalDateTime.now()
        );

        Visit completed = new Visit(
            visit.id(), visit.bookingId(), visit.providerId(), visit.patientId(),
            vitals, request.clinicalNotes(),
            request.proceduresPerformed() != null ? request.proceduresPerformed() : List.of(),
            request.photos() != null ? request.photos() : List.of(),
            request.recommendations() != null ? List.of(request.recommendations()) : List.of(),
            request.patientSignature(),
            VisitStatus.COMPLETED, visit.reportUrl(),
            visit.startedAt(), LocalDateTime.now(),
            visit.createdAt(), LocalDateTime.now()
        );

        log.info("Visit {} completed", visitId);
        return toResponse(completed);
    }

    @Transactional
    public EscalationResponse escalateVisit(UUID visitId, EscalationRequest request) {
        Visit visit = getOrCreateMockVisit(toLong(visitId));

        log.info("Escalation created for visit {} - type: {}", visitId, request.urgencyType());

        return new EscalationResponse(
            System.currentTimeMillis(),
            toLong(visitId),
            request.urgencyType(),
            request.notes(),
            request.gpsLocation(),
            LocalDateTime.now(),
            List.of("emergency_services", "platform_ops")
        );
    }

    @Transactional(readOnly = true)
    public Resource getVisitPdf(UUID visitId) {
        Visit visit = getOrCreateMockVisit(toLong(visitId));

        if (!visit.isCompleted()) {
            throw new ValidationException("PDF only available for completed visits");
        }

        // In production, this would generate PDF from S3 or cache
        byte[] pdfContent = "PDF content for visit".getBytes();
        return new ByteArrayResource(pdfContent);
    }

    @Transactional
    public void sendReportToPatient(UUID visitId) {
        Visit visit = getOrCreateMockVisit(toLong(visitId));

        if (!visit.isCompleted()) {
            throw new ValidationException("Report only available for completed visits");
        }

        // In production, queue email via SendGrid
        log.info("Report email queued for visit {} to patient {}", visitId, visit.patientId());
    }

    @Transactional
    public RatingResponse rateVisit(UUID visitId, int rating, String review) {
        Visit visit = getOrCreateMockVisit(toLong(visitId));

        if (!visit.isCompleted()) {
            throw new ValidationException("Can only rate completed visits");
        }

        log.info("Rating submitted for visit {}: {} stars", visitId, rating);
        return new RatingResponse(System.currentTimeMillis(), rating, review);
    }

    public record RatingResponse(Long id, int rating, String review) {}

    private Visit getOrCreateMockVisit(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return new Visit(
            id != null ? id : 1L,
            1L, 2L, 1L,
            new Vitals("120/80", 72, 36.6, 98, null, null, null, null, now),
            "Patient stable, wound healing well. Changed dressing.",
            List.of("Wound dressing change", "Vital signs check"),
            List.of(),
            List.of("Continue medication", "Rest for 2 days"),
            null,
            VisitStatus.COMPLETED,
            "https://s3.zdravdom.com/reports/visit-" + id + ".pdf",
            now.minusHours(2),
            now.minusHours(1),
            now.minusHours(2),
            now
        );
    }

    private VisitResponse toResponse(Visit visit) {
        return new VisitResponse(
            visit.id(), visit.bookingId(), visit.providerId(), visit.patientId(),
            visit.vitals(), visit.clinicalNotes(), visit.proceduresPerformed(),
            visit.photos(), visit.recommendations(), visit.patientSignature(),
            visit.status(), visit.reportUrl(), visit.startedAt(), visit.completedAt()
        );
    }

    private Long toLong(UUID uuid) {
        return uuid != null ? uuid.getMostSignificantBits() : null;
    }
}