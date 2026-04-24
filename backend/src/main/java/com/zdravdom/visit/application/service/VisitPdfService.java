package com.zdravdom.visit.application.service;

import com.zdravdom.visit.domain.Visit;
import com.zdravdom.visit.domain.Vitals;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Generates PDF visit reports using PDFBox.
 * Produces a clinical visit summary with vitals, notes, and procedures.
 */
@Service
public class VisitPdfService {

    private static final Logger log = LoggerFactory.getLogger(VisitPdfService.class);
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Generate a PDF report for a completed visit.
     *
     * @return PDF bytes
     */
    public byte[] generateVisitReport(Visit visit, Vitals vitals, String patientName, String providerName) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 14.5f;

                // ── Title ────────────────────────────────────────────────────────
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.newLineAtOffset(margin, y);
                cs.showText("Zdravdom - Clinical Visit Report");
                cs.endText();
                y -= 30;

                // ── Visit Info ──────────────────────────────────────────────────
                y = drawSection(cs, margin, y, leading, "Visit Information");
                y = drawKeyValue(cs, margin + 10, y, leading, "Visit ID:", String.valueOf(visit.getId()));
                y = drawKeyValue(cs, margin + 10, y, leading, "Date:",
                        visit.getStartedAt() != null ? visit.getStartedAt().format(DT_FORMAT) : "N/A");
                y = drawKeyValue(cs, margin + 10, y, leading, "Patient:", patientName != null ? patientName : "N/A");
                y = drawKeyValue(cs, margin + 10, y, leading, "Provider:", providerName != null ? providerName : "N/A");
                y = drawKeyValue(cs, margin + 10, y, leading, "Status:", visit.getStatus() != null ? visit.getStatus().name() : "N/A");
                y -= 15;

                // ── Vitals ──────────────────────────────────────────────────────
                if (vitals != null) {
                    y = drawSection(cs, margin, y, leading, "Vital Signs");
                    if (vitals.getBloodPressure() != null)
                        y = drawKeyValue(cs, margin + 10, y, leading, "Blood Pressure:", vitals.getBloodPressure());
                    if (vitals.getHeartRate() != null)
                        y = drawKeyValue(cs, margin + 10, y, leading, "Heart Rate:", vitals.getHeartRate() + " bpm");
                    if (vitals.getTemperature() != null)
                        y = drawKeyValue(cs, margin + 10, y, leading, "Temperature:", vitals.getTemperature() + " C");
                    if (vitals.getO2Saturation() != null)
                        y = drawKeyValue(cs, margin + 10, y, leading, "O2 Saturation:", vitals.getO2Saturation() + " %");
                    if (vitals.getRespiratoryRate() != null)
                        y = drawKeyValue(cs, margin + 10, y, leading, "Respiratory Rate:", vitals.getRespiratoryRate() + " /min");
                    if (vitals.getBloodGlucose() != null)
                        y = drawKeyValue(cs, margin + 10, y, leading, "Blood Glucose:", vitals.getBloodGlucose() + " mg/dL");
                    if (vitals.getWeight() != null)
                        y = drawKeyValue(cs, margin + 10, y, leading, "Weight:", vitals.getWeight() + " kg");
                    if (vitals.getNotes() != null && !vitals.getNotes().isBlank())
                        y = drawKeyValue(cs, margin + 10, y, leading, "Notes:", vitals.getNotes());
                    y -= 10;
                }

                // ── Clinical Notes ────────────────────────────────────────────
                if (visit.getClinicalNotes() != null && !visit.getClinicalNotes().isBlank()) {
                    y = drawSection(cs, margin, y, leading, "Clinical Notes");
                    y = drawMultiline(cs, margin + 10, y, leading, visit.getClinicalNotes());
                    y -= 10;
                }

                // ── Procedures ────────────────────────────────────────────────
                if (visit.getProceduresPerformed() != null && !visit.getProceduresPerformed().isEmpty()) {
                    y = drawSection(cs, margin, y, leading, "Procedures Performed");
                    for (String proc : visit.getProceduresPerformed()) {
                        y = drawBullet(cs, margin + 10, y, leading, proc);
                    }
                    y -= 10;
                }

                // ── Recommendations ───────────────────────────────────────────
                if (visit.getRecommendations() != null && !visit.getRecommendations().isEmpty()) {
                    y = drawSection(cs, margin, y, leading, "Recommendations");
                    for (String rec : visit.getRecommendations()) {
                        y = drawBullet(cs, margin + 10, y, leading, rec);
                    }
                    y -= 10;
                }

                // ── Signature ─────────────────────────────────────────────────
                if (visit.getPatientSignature() != null && !visit.getPatientSignature().isBlank()) {
                    y = drawSection(cs, margin, y, leading, "Patient Signature");
                    y = drawKeyValue(cs, margin + 10, y, leading, "Signature:", visit.getPatientSignature());
                }

                // ── Footer ─────────────────────────────────────────────────────
                float footerY = margin;
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                cs.newLineAtOffset(margin, footerY);
                cs.showText("Generated by Zdravdom | Confidential healthcare document");
                cs.endText();
            }

            document.save(out);
            log.info("Generated PDF report for visit {}", visit.getId());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate PDF for visit {}: {}", visit.getId(), e.getMessage());
            throw new RuntimeException("PDF generation failed for visit " + visit.getId(), e);
        }
    }

    // ─── Drawing helpers ───────────────────────────────────────────────────────

    private float drawSection(PDPageContentStream cs, float x, float y,
                               float leading, String title) throws IOException {
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
        cs.newLineAtOffset(x, y);
        cs.showText(title);
        cs.endText();
        return y - leading * 1.5f;
    }

    private float drawKeyValue(PDPageContentStream cs, float x, float y,
                                float leading, String key, String value) throws IOException {
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
        cs.newLineAtOffset(x, y);
        cs.showText(key + " ");
        float keyWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD).getStringWidth(key + " ") / 1000 * 10;
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.newLineAtOffset(x + keyWidth, y);
        cs.showText(value != null ? value : "N/A");
        cs.endText();
        return y - leading;
    }

    private float drawBullet(PDPageContentStream cs, float x, float y,
                             float leading, String text) throws IOException {
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.newLineAtOffset(x, y);
        cs.showText("- " + text);
        cs.endText();
        return y - leading;
    }

    private float drawMultiline(PDPageContentStream cs, float x, float y,
                                float leading, String text) throws IOException {
        // PRODUCTION: Text truncation at 80 chars without word-wrap produces ugly PDFs — use PDFTextStripWriter for proper multi-line text wrapping
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.newLineAtOffset(x, y);
        cs.showText(text.length() > 80 ? text.substring(0, 80) : text);
        cs.endText();
        return y - leading;
    }
}
