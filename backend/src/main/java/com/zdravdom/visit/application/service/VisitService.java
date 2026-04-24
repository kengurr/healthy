package com.zdravdom.visit.application.service;

import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.visit.adapters.out.persistence.EscalationRepository;
import com.zdravdom.visit.adapters.out.persistence.VisitRatingRepository;
import com.zdravdom.visit.adapters.out.persistence.VisitRepository;
import com.zdravdom.visit.adapters.out.persistence.VitalsRepository;
import com.zdravdom.visit.application.dto.CompleteVisitRequest;
import com.zdravdom.visit.application.dto.EscalationRequest;
import com.zdravdom.visit.application.dto.EscalationResponse;
import com.zdravdom.visit.application.dto.VitalsResponse;
import com.zdravdom.visit.application.dto.VisitResponse;
import com.zdravdom.visit.domain.Escalation;
import com.zdravdom.visit.domain.Vitals;
import com.zdravdom.visit.domain.Visit;
import com.zdravdom.visit.domain.Visit.VisitStatus;
import com.zdravdom.visit.domain.VisitRating;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for visit management.
 */
@Service
public class VisitService {

    private static final Logger log = LoggerFactory.getLogger(VisitService.class);

    private final VisitRepository visitRepository;
    private final VitalsRepository vitalsRepository;
    private final VisitRatingRepository ratingRepository;
    private final EscalationRepository escalationRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final VisitPdfService visitPdfService;
    private final JavaMailSender mailSender;

    public VisitService(
            VisitRepository visitRepository,
            VitalsRepository vitalsRepository,
            VisitRatingRepository ratingRepository,
            EscalationRepository escalationRepository,
            PatientRepository patientRepository,
            ProviderRepository providerRepository,
            VisitPdfService visitPdfService,
            JavaMailSender mailSender) {
        this.visitRepository = visitRepository;
        this.vitalsRepository = vitalsRepository;
        this.ratingRepository = ratingRepository;
        this.escalationRepository = escalationRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.visitPdfService = visitPdfService;
        this.mailSender = mailSender;
    }

    @Transactional(readOnly = true)
    public VisitResponse getVisitById(Long visitId) {
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit", visitId));
        return toResponse(visit);
    }

    @Transactional
    public VisitResponse startVisit(Long visitId, Long providerId) {
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit", visitId));

        if (!visit.isInProgress()) {
            throw new ValidationException("Visit cannot be started in current status: " + visit.getStatus());
        }

        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setStartedAt(LocalDateTime.now());
        visit.setProviderId(providerId);

        Visit saved = visitRepository.save(visit);
        log.info("Visit {} started by provider {}", visitId, providerId);
        return toResponse(saved);
    }

    @Transactional
    public VisitResponse completeVisit(Long visitId, CompleteVisitRequest request) {
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit", visitId));

        if (!visit.isInProgress()) {
            throw new ValidationException("Visit cannot be completed in current status: " + visit.getStatus());
        }

        Vitals vitals = Vitals.create();
        vitals.setVisitId(visitId);
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
        vitalsRepository.save(vitals);

        visit.setClinicalNotes(request.clinicalNotes());
        visit.setProceduresPerformed(request.proceduresPerformed() != null ? request.proceduresPerformed() : List.of());
        visit.setPhotos(request.photos() != null ? request.photos() : List.of());
        visit.setRecommendations(request.recommendations() != null ? List.of(request.recommendations()) : List.of());
        visit.setPatientSignature(request.patientSignature());
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setCompletedAt(LocalDateTime.now());

        Visit saved = visitRepository.save(visit);
        log.info("Visit {} completed", visitId);
        return toResponse(saved);
    }

    @Transactional
    public EscalationResponse escalateVisit(Long visitId, EscalationRequest request) {
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit", visitId));

        Escalation escalation = Escalation.create();
        escalation.setVisitId(visitId);
        escalation.setProviderId(visit.getProviderId());
        escalation.setPatientId(visit.getPatientId());
        escalation.setUrgencyType(request.urgencyType());
        escalation.setDescription(request.notes());
        if (request.gpsLocation() != null) {
            escalation.setGpsLat(request.gpsLocation().lat());
            escalation.setGpsLng(request.gpsLocation().lng());
        }

        // Set notified users based on urgency type
        // PRODUCTION: Notified users must come from a configurable escalation matrix (urgency → role → notification channel)
        List<String> notifiedUsers = escalation.requiresEmergencyServices()
            ? List.of("emergency_services", "platform_ops", "on_call_manager")
            : List.of("platform_ops", "on_call_manager");
        escalation.setNotifiedUsers(notifiedUsers);

        Escalation saved = escalationRepository.save(escalation);
        log.info("Escalation {} created for visit {} - type: {}", saved.getId(), visitId, request.urgencyType());

        return new EscalationResponse(
            saved.getId(), visitId, request.urgencyType(), request.notes(),
            request.gpsLocation(), saved.getCreatedAt(), notifiedUsers);
    }

    @Transactional(readOnly = true)
    public Resource getVisitPdf(Long visitId) {
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit", visitId));

        if (!visit.isCompleted()) {
            throw new ValidationException("PDF only available for completed visits");
        }

        String patientName = resolvePatientName(visit.getPatientId());
        String providerName = resolveProviderName(visit.getProviderId());
        Vitals vitals = vitalsRepository.findByVisitId(visitId).orElse(null);
        byte[] pdfBytes = visitPdfService.generateVisitReport(visit, vitals, patientName, providerName);

        return new ByteArrayResource(pdfBytes);
    }

    @Transactional
    public void sendReportToPatient(Long visitId) {
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit", visitId));

        if (!visit.isCompleted()) {
            throw new ValidationException("Report only available for completed visits");
        }

        Patient patient = patientRepository.findById(visit.getPatientId()).orElse(null);
        if (patient == null) {
            throw new ResourceNotFoundException("Patient", visit.getPatientId());
        }

        String patientEmail = patient.getEmail();
        if (patientEmail == null || patientEmail.isBlank()) {
            throw new ValidationException("Patient has no email address on file");
        }

        String patientName = patient.getFirstName() + " " + patient.getLastName();
        String providerName = resolveProviderName(visit.getProviderId());
        Vitals vitals = vitalsRepository.findByVisitId(visitId).orElse(null);
        byte[] pdfBytes = visitPdfService.generateVisitReport(visit, vitals, patientName, providerName);

        sendEmailWithPdf(patientEmail, patientName, visitId, pdfBytes);
        log.info("Visit report email queued for visit {} to patient {}", visitId, patientEmail);
    }

    @Transactional
    public RatingResponse rateVisit(Long visitId, int rating, String review) {
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit", visitId));

        if (!visit.isCompleted()) {
            throw new ValidationException("Can only rate completed visits");
        }

        if (ratingRepository.existsByVisitId(visitId)) {
            throw new ValidationException("Visit already rated");
        }

        VisitRating visitRating = VisitRating.create();
        visitRating.setVisitId(visitId);
        visitRating.setPatientId(visit.getPatientId());
        visitRating.setProviderId(visit.getProviderId());
        visitRating.setRating(rating);
        visitRating.setReview(review);

        VisitRating saved = ratingRepository.save(visitRating);
        log.info("Rating submitted for visit {}: {} stars", visitId, rating);
        return new RatingResponse(saved.getId(), rating, review);
    }

    public record RatingResponse(Long id, int rating, String review) {}

    // ─── Email ─────────────────────────────────────────────────────────────────

    private void sendEmailWithPdf(String toEmail, String patientName, Long visitId, byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your Zdravdom Visit Report – Visit #" + visitId);
            helper.setText("""
                Dear %s,

                Please find attached your clinical visit report from Zdravdom.

                This document contains confidential health information. Please keep it secure.

                If you have any questions about your treatment, contact your healthcare provider.

                Best regards,
                Zdravdom Healthcare Team
                """.formatted(patientName));

            helper.addAttachment("visit-report-" + visitId + ".pdf", new ByteArrayResource(pdfBytes));
            // PRODUCTION: Email sending must be async (@Async or message queue) — blocks DB transaction if inside @Transactional
            mailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send visit report email for visit {} to {}: {}", visitId, toEmail, e.getMessage());
            throw new RuntimeException("Failed to send report email for visit " + visitId, e);
        }
    }

    // ─── Response mapping ─────────────────────────────────────────────────────

    private VisitResponse toResponse(Visit visit) {
        VitalsResponse vitalsResponse = vitalsRepository.findByVisitId(visit.getId())
            .map(this::toVitalsResponse)
            .orElse(null);

        return new VisitResponse(
            visit.getId(), visit.getBookingId(), visit.getProviderId(), visit.getPatientId(),
            vitalsResponse, visit.getClinicalNotes(), visit.getProceduresPerformed(),
            visit.getPhotos(), visit.getRecommendations(), visit.getPatientSignature(),
            visit.getStatus(), visit.getReportUrl(), visit.getStartedAt(), visit.getCompletedAt()
        );
    }

    private VitalsResponse toVitalsResponse(Vitals vitals) {
        return new VitalsResponse(
            vitals.getId(), vitals.getVisitId(), vitals.getBloodPressure(),
            vitals.getHeartRate(), vitals.getTemperature(), vitals.getO2Saturation(),
            vitals.getRespiratoryRate(), vitals.getBloodGlucose(), vitals.getWeight(),
            vitals.getNotes(), vitals.getRecordedAt()
        );
    }

    private String resolvePatientName(Long patientId) {
        return patientRepository.findById(patientId)
            .map(p -> p.getFirstName() + " " + p.getLastName())
            .orElse("Unknown Patient");
    }

    private String resolveProviderName(Long providerId) {
        return providerRepository.findById(providerId)
            .map(p -> p.getFirstName() + " " + p.getLastName())
            .orElse("Unknown Provider");
    }
}
