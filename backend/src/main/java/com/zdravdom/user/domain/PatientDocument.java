package com.zdravdom.user.domain;

import java.time.LocalDateTime;

/**
 * Patient document for GDPR-related files (ID, insurance cards, referrals, etc.).
 */
public record PatientDocument(
    Long id,
    Long patientId,
    DocumentType documentType,
    String fileName,
    String s3Key,
    String mimeType,
    Long fileSize,
    LocalDateTime uploadedAt,
    LocalDateTime expiresAt,
    boolean verified,
    String verifiedBy,
    LocalDateTime verifiedAt
) {
    public enum DocumentType {
        NATIONAL_ID,
        INSURANCE_CARD,
        REFERRAL,
        MEDICAL_HISTORY,
        CONSENT_FORM,
        OTHER
    }

    public PatientDocument {
        if (documentType == null) {
            throw new IllegalArgumentException("Document type cannot be null");
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean needsVerification() {
        return !verified && !isExpired();
    }
}
