package com.zdravdom.visit.application.service;

import com.zdravdom.visit.domain.Visit;
import com.zdravdom.visit.domain.Vitals;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisitPdfServiceTest {

    private final VisitPdfService pdfService = new VisitPdfService();

    @Nested
    @DisplayName("generateVisitReport()")
    class GenerateVisitReport {

        @Test
        @DisplayName("generates non-empty PDF bytes")
        void generatesNonEmptyPdf() {
            Visit visit = createVisit(1L, 100L, 200L);
            Vitals vitals = createVitals(1L, 1L);

            byte[] pdf = pdfService.generateVisitReport(visit, vitals, "Jane Doe", "Dr. Smith");

            assertThat(pdf).isNotEmpty();
            assertThat(isValidPdf(pdf)).isTrue();
        }

        @Test
        @DisplayName("generates PDF when vitals is null")
        void generatesPdfWithNullVitals() {
            Visit visit = createVisit(1L, 100L, 200L);
            visit.setClinicalNotes("Patient reported headache and fatigue.");

            byte[] pdf = pdfService.generateVisitReport(visit, null, "Jane Doe", "Dr. Smith");

            assertThat(pdf).isNotEmpty();
            assertThat(isValidPdf(pdf)).isTrue();
        }

        @Test
        @DisplayName("generates PDF when visit has procedures and recommendations")
        void generatesPdfWithProceduresAndRecommendations() {
            Visit visit = createVisit(1L, 100L, 200L);
            visit.setProceduresPerformed(List.of("Blood draw", "Blood pressure check"));
            visit.setRecommendations(List.of("Rest for 2 days", "Increase fluid intake"));

            byte[] pdf = pdfService.generateVisitReport(visit, null, "Jane Doe", "Dr. Smith");

            assertThat(pdf).isNotEmpty();
            assertThat(isValidPdf(pdf)).isTrue();
        }

        @Test
        @DisplayName("generates PDF when patient has signature")
        void generatesPdfWithSignature() {
            Visit visit = createVisit(1L, 100L, 200L);
            visit.setPatientSignature("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

            byte[] pdf = pdfService.generateVisitReport(visit, null, "Jane Doe", "Dr. Smith");

            assertThat(pdf).isNotEmpty();
            assertThat(isValidPdf(pdf)).isTrue();
        }

        @Test
        @DisplayName("generates PDF with null patient/provider names")
        void generatesPdfWithNullNames() {
            Visit visit = createVisit(1L, 100L, 200L);

            byte[] pdf = pdfService.generateVisitReport(visit, null, null, null);

            assertThat(pdf).isNotEmpty();
            assertThat(isValidPdf(pdf)).isTrue();
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Visit createVisit(Long id, Long providerId, Long patientId) {
        Visit visit = Visit.create();
        visit.setId(id);
        visit.setProviderId(providerId);
        visit.setPatientId(patientId);
        visit.setBookingId(10L);
        visit.setStatus(Visit.VisitStatus.COMPLETED);
        visit.setStartedAt(LocalDateTime.now().minusHours(1));
        visit.setCompletedAt(LocalDateTime.now());
        visit.setClinicalNotes("Routine check-up");
        return visit;
    }

    private Vitals createVitals(Long id, Long visitId) {
        Vitals v = Vitals.create();
        v.setId(id);
        v.setVisitId(visitId);
        v.setBloodPressure("120/80");
        v.setHeartRate(72);
        v.setTemperature(BigDecimal.valueOf(36.6));
        v.setO2Saturation(97);
        v.setRespiratoryRate(16);
        v.setBloodGlucose(BigDecimal.valueOf(95));
        v.setWeight(BigDecimal.valueOf(70));
        v.setNotes("Patient feeling well");
        v.setRecordedAt(LocalDateTime.now());
        return v;
    }

    private boolean isValidPdf(byte[] bytes) {
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            return doc.getNumberOfPages() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
