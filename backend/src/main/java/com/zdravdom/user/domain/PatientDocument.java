package com.zdravdom.user.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Patient document for GDPR-related files (ID, insurance cards, referrals, etc.).
 * Maps to user.patient_documents table.
 */
@Entity
@Table(name = "patient_documents", schema = "`user`")
public class PatientDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private boolean verified = false;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public enum DocumentType {
        NATIONAL_ID, INSURANCE_CARD, REFERRAL, MEDICAL_HISTORY, CONSENT_FORM, OTHER
    }

    // Default constructor for JPA
    public PatientDocument() {}

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean needsVerification() {
        return !verified && !isExpired();
    }

    // Getters
    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public DocumentType getDocumentType() { return documentType; }
    public String getFileName() { return fileName; }
    public String getS3Key() { return s3Key; }
    public String getMimeType() { return mimeType; }
    public Long getFileSize() { return fileSize; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isVerified() { return verified; }
    public String getVerifiedBy() { return verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
