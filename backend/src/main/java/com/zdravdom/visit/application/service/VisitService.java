package com.zdravdom.visit.application.service;

import com.zdravdom.visit.application.dto.*;
import com.zdravdom.visit.application.dto.CompleteVisitRequest.VitalsRequest;
import com.zdravdom.visit.domain.Visit;
import com.zdravdom.visit.domain.Visit.VisitStatus;
import com.zdravdom.visit.domain.Vitals;

import java.math.BigDecimal;
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

/**
 * Service for visit management.
 */
@Service
public class VisitService {

    private static final Logger log = LoggerFactory.getLogger(VisitService.class);

    @Transactional(readOnly = true)
    public VisitResponse getVisitById(Long visitId) {
        Visit visit = getOrCreateMockVisit(visitId);
        return toResponse(visit);
    }

    @Transactional
    public VisitResponse startVisit(Long visitId, Long providerId) {
        Visit visit = getOrCreateMockVisit(visitId);

        if (!visit.isInProgress()) {
            throw new ValidationException("Visit cannot be started in current status");
        }

        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setStartedAt(LocalDateTime.now());
        visit.setProviderId(providerId);

        log.info("Visit {} started by provider {}", visitId, providerId);
        return toResponse(visit);
    }

    @Transactional
    public VisitResponse completeVisit(Long visitId, CompleteVisitRequest request) {
        Visit visit = getOrCreateMockVisit(visitId);

        Vitals vitals = new Vitals();
        if (request.vitals() != null) {
            vitals.setBloodPressure(request.vitals().bloodPressure());
            vitals.setHeartRate(request.vitals().heartRate());
            if (request.vitals().temperature() != null) {
                vitals.setTemperature(BigDecimal.valueOf(request.vitals().temperature()));
            }
            vitals.setO2Saturation(request.vitals().o2Saturation());
            vitals.setRespiratoryRate(request.vitals().respiratoryRate());
            if (request.vitals().bloodGlucose() != null) {
                vitals.setBloodGlucose(BigDecimal.valueOf(request.vitals().bloodGlucose()));
            }
            if (request.vitals().weight() != null) {
                vitals.setWeight(BigDecimal.valueOf(request.vitals().weight()));
            }
            vitals.setNotes(request.vitals().notes());
        }
        vitals.setRecordedAt(LocalDateTime.now());
        visit.setVitals(vitals);

        visit.setClinicalNotes(request.clinicalNotes());
        visit.setProceduresPerformed(request.proceduresPerformed() != null ? request.proceduresPerformed() : List.of());
        visit.setPhotos(request.photos() != null ? request.photos() : List.of());
        visit.setRecommendations(request.recommendations() != null ? List.of(request.recommendations()) : List.of());
        visit.setPatientSignature(request.patientSignature());
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setCompletedAt(LocalDateTime.now());

        log.info("Visit {} completed", visitId);
        return toResponse(visit);
    }

    @Transactional
    public EscalationResponse escalateVisit(Long visitId, EscalationRequest request) {
        Visit visit = getOrCreateMockVisit(visitId);

        log.info("Escalation created for visit {} - type: {}", visitId, request.urgencyType());

        return new EscalationResponse(
            System.currentTimeMillis(),
            visitId,
            request.urgencyType(),
            request.notes(),
            request.gpsLocation(),
            LocalDateTime.now(),
            List.of("emergency_services", "platform_ops")
        );
    }

    @Transactional(readOnly = true)
    public Resource getVisitPdf(Long visitId) {
        Visit visit = getOrCreateMockVisit(visitId);

        if (!visit.isCompleted()) {
            throw new ValidationException("PDF only available for completed visits");
        }

        // In production, this would generate PDF from S3 or cache
        byte[] pdfContent = "PDF content for visit".getBytes();
        return new ByteArrayResource(pdfContent);
    }

    @Transactional
    public void sendReportToPatient(Long visitId) {
        Visit visit = getOrCreateMockVisit(visitId);

        if (!visit.isCompleted()) {
            throw new ValidationException("Report only available for completed visits");
        }

        // In production, queue email via SendGrid
        log.info("Report email queued for visit {} to patient {}", visitId, visit.getPatientId());
    }

    @Transactional
    public RatingResponse rateVisit(Long visitId, int rating, String review) {
        Visit visit = getOrCreateMockVisit(visitId);

        if (!visit.isCompleted()) {
            throw new ValidationException("Can only rate completed visits");
        }

        log.info("Rating submitted for visit {}: {} stars", visitId, rating);
        return new RatingResponse(System.currentTimeMillis(), rating, review);
    }

    public record RatingResponse(Long id, int rating, String review) {}

    private Visit getOrCreateMockVisit(Long id) {
        LocalDateTime now = LocalDateTime.now();
        Visit visit = new Visit();
        visit.setId(id != null ? id : 1L);
        visit.setBookingId(1L);
        visit.setProviderId(2L);
        visit.setPatientId(1L);
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setReportUrl("https://s3.zdravdom.com/reports/visit-" + id + ".pdf");
        visit.setStartedAt(now.minusHours(2));
        visit.setCompletedAt(now.minusHours(1));
        visit.setCreatedAt(now.minusHours(2));
        visit.setUpdatedAt(now);

        Vitals vitals = new Vitals();
        vitals.setBloodPressure("120/80");
        vitals.setHeartRate(72);
        vitals.setTemperature(BigDecimal.valueOf(36.6));
        vitals.setO2Saturation(98);
        vitals.setRecordedAt(now);
        visit.setVitals(vitals);

        visit.setClinicalNotes("Patient stable, wound healing well. Changed dressing.");
        visit.setProceduresPerformed(List.of("Wound dressing change", "Vital signs check"));
        visit.setPhotos(List.of());
        visit.setRecommendations(List.of("Continue medication", "Rest for 2 days"));

        return visit;
    }

    private VisitResponse toResponse(Visit visit) {
        return new VisitResponse(
            visit.getId(), visit.getBookingId(), visit.getProviderId(), visit.getPatientId(),
            visit.getVitals(), visit.getClinicalNotes(), visit.getProceduresPerformed(),
            visit.getPhotos(), visit.getRecommendations(), visit.getPatientSignature(),
            visit.getStatus(), visit.getReportUrl(), visit.getStartedAt(), visit.getCompletedAt()
        );
    }
}
