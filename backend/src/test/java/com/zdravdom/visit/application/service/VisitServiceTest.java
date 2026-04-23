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
import com.zdravdom.visit.application.dto.VisitResponse;
import com.zdravdom.visit.domain.Escalation;
import com.zdravdom.visit.domain.Visit;
import com.zdravdom.visit.domain.VisitRating;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock private VisitRepository visitRepository;
    @Mock private VitalsRepository vitalsRepository;
    @Mock private VisitRatingRepository ratingRepository;
    @Mock private EscalationRepository escalationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private ProviderRepository providerRepository;
    @Mock private VisitPdfService visitPdfService;
    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    private VisitService visitService;

    @BeforeEach
    void setUp() {
        visitService = new VisitService(
            visitRepository, vitalsRepository, ratingRepository, escalationRepository,
            patientRepository, providerRepository, visitPdfService, mailSender);
    }

    // ─── startVisit ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startVisit()")
    class StartVisit {

        @Test
        @DisplayName("starts visit and returns updated response")
        void startsVisitSuccessfully() {
            Visit visit = createVisit(1L, Visit.VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(visitRepository.save(any(Visit.class))).thenAnswer(i -> i.getArgument(0));

            VisitResponse response = visitService.startVisit(1L, 100L);

            assertThat(response.status()).isEqualTo(Visit.VisitStatus.IN_PROGRESS);
            assertThat(response.startedAt()).isNotNull();
            verify(visitRepository).save(any(Visit.class));
        }

        @Test
        @DisplayName("throws when visit not found")
        void throwsWhenNotFound() {
            when(visitRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitService.startVisit(999L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws when visit is not in progress")
        void throwsWhenNotInProgress() {
            Visit visit = createVisit(1L, Visit.VisitStatus.COMPLETED);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            assertThatThrownBy(() -> visitService.startVisit(1L, 100L))
                .isInstanceOf(ValidationException.class);
        }
    }

    // ─── completeVisit ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeVisit()")
    class CompleteVisit {

        @Test
        @DisplayName("completes visit and persists vitals")
        void completesVisitWithVitals() {
            Visit visit = createVisit(1L, Visit.VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(visitRepository.save(any(Visit.class))).thenAnswer(i -> i.getArgument(0));
            when(vitalsRepository.save(any())).thenAnswer(i -> {
                var v = i.getArgument(0, com.zdravdom.visit.domain.Vitals.class);
                v.setId(10L);
                return v;
            });

            CompleteVisitRequest.VitalsRequest vitalsReq = new CompleteVisitRequest.VitalsRequest(
                "130/85", 75, 36.8, 97, 14, 100.5, 72.0, "Patient feeling well");
            CompleteVisitRequest request = new CompleteVisitRequest(
                vitalsReq, "Routine check", List.of("Blood draw"), List.of("photo1.jpg"),
                "Rest and fluids", "patient-signature-base64");

            VisitResponse response = visitService.completeVisit(1L, request);

            assertThat(response.status()).isEqualTo(Visit.VisitStatus.COMPLETED);
            assertThat(response.completedAt()).isNotNull();
            assertThat(response.clinicalNotes()).isEqualTo("Routine check");

            verify(vitalsRepository).save(any(com.zdravdom.visit.domain.Vitals.class));
        }

        @Test
        @DisplayName("throws when visit not found")
        void throwsWhenNotFound() {
            when(visitRepository.findById(999L)).thenReturn(Optional.empty());

            CompleteVisitRequest request = new CompleteVisitRequest(null, null, null, null, null, null);
            assertThatThrownBy(() -> visitService.completeVisit(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── escalateVisit ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("escalateVisit()")
    class EscalateVisit {

        @Test
        @DisplayName("creates escalation with emergency services notified")
        void createsEscalationWithEmergency() {
            Visit visit = createVisit(1L, Visit.VisitStatus.IN_PROGRESS);
            visit.setProviderId(100L);
            visit.setPatientId(200L);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(escalationRepository.save(any(Escalation.class))).thenAnswer(i -> {
                Escalation e = i.getArgument(0);
                e.setId(5L);
                e.setCreatedAt(LocalDateTime.now());
                return e;
            });

            EscalationRequest request = new EscalationRequest(
                Escalation.UrgencyType.MEDICAL_EMERGENCY, "Patient experiencing chest pain",
                new EscalationRequest.GpsLocation(46.05, 14.05));

            var response = visitService.escalateVisit(1L, request);

            assertThat(response.urgencyType()).isEqualTo(Escalation.UrgencyType.MEDICAL_EMERGENCY);
            assertThat(response.notifiedUsers()).contains("emergency_services");

            ArgumentCaptor<Escalation> escCaptor = ArgumentCaptor.forClass(Escalation.class);
            verify(escalationRepository).save(escCaptor.capture());
            Escalation saved = escCaptor.getValue();
            assertThat(saved.getGpsLat()).isEqualTo(46.05);
            assertThat(saved.getGpsLng()).isEqualTo(14.05);
        }
    }

    // ─── rateVisit ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rateVisit()")
    class RateVisit {

        @Test
        @DisplayName("submits rating and returns response")
        void submitsRatingSuccessfully() {
            Visit visit = createVisit(1L, Visit.VisitStatus.COMPLETED);
            visit.setPatientId(200L);
            visit.setProviderId(100L);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(ratingRepository.existsByVisitId(1L)).thenReturn(false);
            when(ratingRepository.save(any(VisitRating.class))).thenAnswer(i -> {
                VisitRating r = i.getArgument(0);
                r.setId(1L);
                return r;
            });

            VisitService.RatingResponse response = visitService.rateVisit(1L, 5, "Excellent care!");

            assertThat(response.rating()).isEqualTo(5);
            assertThat(response.review()).isEqualTo("Excellent care!");
        }

        @Test
        @DisplayName("throws when visit already rated")
        void throwsWhenAlreadyRated() {
            Visit visit = createVisit(1L, Visit.VisitStatus.COMPLETED);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(ratingRepository.existsByVisitId(1L)).thenReturn(true);

            assertThatThrownBy(() -> visitService.rateVisit(1L, 5, "Great!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already rated");
        }
    }

    // ─── sendReportToPatient ────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendReportToPatient()")
    class SendReportToPatient {

        @Test
        @DisplayName("sends email with PDF attachment")
        void sendsEmailWithPdf() throws Exception {
            Visit visit = createVisit(1L, Visit.VisitStatus.COMPLETED);
            visit.setPatientId(200L);
            visit.setProviderId(100L);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            Patient patient = new Patient();
            patient.setId(200L);
            patient.setEmail("patient@example.com");
            patient.setFirstName("Jane");
            patient.setLastName("Doe");
            when(patientRepository.findById(200L)).thenReturn(Optional.of(patient));

            Provider provider = new Provider();
            provider.setFirstName("Dr.");
            provider.setLastName("Smith");
            when(providerRepository.findById(100L)).thenReturn(Optional.of(provider));

            when(visitPdfService.generateVisitReport(any(), any(), any(), any()))
                .thenReturn("PDF bytes".getBytes());
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            visitService.sendReportToPatient(1L);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("throws when patient has no email")
        void throwsWhenNoEmail() {
            Visit visit = createVisit(1L, Visit.VisitStatus.COMPLETED);
            visit.setPatientId(200L);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            Patient patient = new Patient();
            patient.setId(200L);
            patient.setEmail(null);
            when(patientRepository.findById(200L)).thenReturn(Optional.of(patient));

            assertThatThrownBy(() -> visitService.sendReportToPatient(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no email");
        }
    }

    // ─── getVisitPdf ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getVisitPdf()")
    class GetVisitPdf {

        @Test
        @DisplayName("returns PDF resource for completed visit")
        void returnsPdfResource() {
            Visit visit = createVisit(1L, Visit.VisitStatus.COMPLETED);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(visitPdfService.generateVisitReport(any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

            var resource = visitService.getVisitPdf(1L);

            assertThat(resource).isNotNull();
            assertThat(resource.exists()).isTrue();
        }

        @Test
        @DisplayName("throws when visit is not completed")
        void throwsWhenNotCompleted() {
            Visit visit = createVisit(1L, Visit.VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            assertThatThrownBy(() -> visitService.getVisitPdf(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PDF only available");
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Visit createVisit(Long id, Visit.VisitStatus status) {
        Visit visit = Visit.create();
        visit.setId(id);
        visit.setBookingId(10L);
        visit.setProviderId(100L);
        visit.setPatientId(200L);
        visit.setStatus(status);
        visit.setStartedAt(LocalDateTime.now().minusHours(1));
        return visit;
    }
}
