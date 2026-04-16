# Zdravdom GDPR Data Flow

## Overview
This document describes the end-to-end data flow for personal and health data under GDPR Article 5(1)(b) purpose limitation and Article 32 security requirements. It covers consent capture through to 10-year retention.

---

## 1. Consent Capture

```
[User Registration / First Login]
         |
         v
+---------------------------+
| Display consent form      |  ← Slovenian + English, plain language
| - Privacy policy link     |  ← Privacy policy v[N] linked
| - Purpose: care delivery  |  ← Explicit description of use
| - Data categories listed  |  ← Health, contact, financial
| - Retention period stated  |  ← 10 years for health data
| - Right to withdraw noted |
+---------------------------+
         |
         | User ticks checkbox (NOT pre-ticked)
         v
+---------------------------+
| Create consent record     |  → auth.consent table
| - user_id (FK)           |
| - consent_type: PRIMARY  |
| - policy_version: "1.0"   |
| - granted: true           |
| - granted_at: TIMESTAMP  |
| - ip_address: TEXT        |
| - user_agent: TEXT        |
| - expires_at: NULL (indefinite until revoked) |
+---------------------------+
         |
         v
+---------------------------+
| Store consent hash        |  → Used to verify active consent
| Generate consent JWT      |    before any data processing
+---------------------------+
```

### Consent Types
| Type | Required | Description |
|------|----------|-------------|
| PRIMARY | Yes | Core care delivery, 10-year health record retention |
| MARKETING | No | Promotional communications |
| ANALYTICS | No | Pseudonymized usage analytics |

---

## 2. Data Storage

### PostgreSQL Schema-per-Module

| Schema | Tables | Data Type |
|--------|--------|-----------|
| `auth` | `user`, `role`, `user_role`, `refresh_token`, `consent`, `mfa_secret` | Auth, identity, consent |
| `user` | `patient_profile`, `provider_profile`, `document`, `address` | PII, verification docs |
| `booking` | `booking`, `booking_provider` | Appointment scheduling |
| `visit` | `visit`, `clinical_form`, `vital_sign`, `media_attachment`, `signature`, `pdf_report` | **Health data** |
| `billing` | `invoice`, `payout`, `stripe_event` | Financial |
| `cms` | `service_package`, `pricing` | Non-personal |
| `analytics` | `event` (pseudonymized) | Aggregated metrics |
| `audit_log` | `audit_entry` | Immutable audit trail |

### Encryption at Rest
- PostgreSQL: Transparent encryption (AWS RDS / Cloud disk encryption)
- S3 (documents, media, reports): SSE-KMS with CMK
- Redis persistence: AES-256 or filesystem-level encryption

### Pseudonymization
- Analytics events use `user_pseudonym` (SHA-256 of user_id + salt); not linked to identity data
- OCR results stored with confidence scores; low-confidence fields flagged for human review

---

## 3. Data Access

### Access Paths
```
[Mobile App / Web Portal] → [REST API] → [Spring Security Filter Chain]
                                              |
                                              v
                                    +---------------------+
                                    | JWT Validation       |
                                    | Role Check           |
                                    | Consent Check        |
                                    +---------------------+
                                              |
                                              v
                              +-------------------------------+
                              | Module (auth/user/visit/etc)  |
                              | Business logic               |
                              | Audit logging                |
                              +-------------------------------+
                                              |
                                              v
                              +-------------------------------+
                              | PostgreSQL / Redis / S3       |
                              | (encrypted at rest)           |
                              +-------------------------------+
```

### Access Controls
- All health data access logged to `audit_log` table (append-only, immutable)
- RBAC enforced at controller level (`@PreAuthorize`)
- Row-level security: queries scoped to `WHERE user_id = :currentUserId` or `WHERE provider_id = :currentProviderId`
- Redis slot locking for concurrent booking access

### Audit Log Entry Fields
```sql
audit_entry_id   BIGINT PRIMARY KEY
user_id          TEXT NOT NULL (pseudonymized in analytics)
action          TEXT NOT NULL  -- READ, CREATE, UPDATE, DELETE, EXPORT
resource_type    TEXT NOT NULL  -- VISIT, CLINICAL_FORM, PATIENT_PROFILE
resource_id     TEXT NOT NULL
ip_address      TEXT
user_agent      TEXT
timestamp       TIMESTAMP NOT NULL DEFAULT NOW()
request_id      TEXT           -- Correlation ID for tracing
```

---

## 4. Data Export (Right of Access — Art. 15)

```
[User: Request Data Export]
         |
         v
+------------------+
| Verify identity  |  ← Requires valid JWT + recent re-auth (password or MFA)
+------------------+
         |
         v
+------------------+
| Check consent    |  ← PRIMARY consent must be active
+------------------+
         |
         v
+------------------+     +------------------------+
| Gather data      | --> | auth: profile, roles   |
|                  | --> | user: addresses, docs  |
|                  | --> | booking: appointments  |
|                  | --> | visit: clinical forms  |
|                  | --> | billing: invoices      |
|                  | --> | audit_log: own entries|
+------------------+
         |
         v
+------------------+     +------------------------+
| Format export    | --> | JSON (machine-readable) |
|                  | --> | PDF (human-readable)    |
|                  | --> | CSV (structured data)   |
+------------------+
         |
         v
+------------------+
| Deliver export   |  ← Download link (signed URL, expires 24h)
|                  |  ← Email notification with link
+------------------+
         |
         v
+------------------+
| Log export event | → audit_log: action=EXPORT
+------------------+
```

- Export fulfilled within **30 days** (target: automated, same day)
- Export file encrypted with user-provided password or temporary key

---

## 5. Data Deletion (Right to Erasure — Art. 17)

### Erasure is Anonymization (Not Physical Deletion)
Health data cannot be physically deleted due to:
1. **Legal obligation**: 10-year health record retention (Healthcare Act / Slovenian law)
2. **Patient safety**: Historical clinical context required for ongoing care

### Anonymization Process
```
[Erasure Request Received]
         |
         v
+------------------+
| Verify identity  |  ← Same as export
+------------------+
         |
         v
+------------------+     +--------------------------------+
| Anonymize PII    | --> | Name → [REDACTED_20260415]      |
|                  | --> | Email → [REDACTED_20260415]     |
|                  | --> | Phone → [REDACTED_20260415]     |
|                  | --> | Address → [REDACTED_20260415]   |
|                  | --> | Date of birth → [REDACTED]      |
+------------------+     +--------------------------------+
         |
         v
+------------------+     +--------------------------------+
| Preserve health  | --> | visit records retained          |
| data            | --> | clinical forms retained         |
|                  | --> | vital signs retained           |
|                  | --> | media attachments retained     |
|                  | --> | signatures retained            |
+------------------+     +--------------------------------+
         |
         v
+------------------+
| Delete derived   |  ← Analytics pseudonymization salt   |
| data             |  ← Marketing profiles                |
+------------------+
         |
         v
+------------------+     +--------------------------------+
| Update consent   | --> | PRIMARY consent revoked        |
| record           | --> | Erasure request logged         |
+------------------+     +--------------------------------+
         |
         v
+------------------+     +--------------------------------+
| Anonymize audit  | --> | audit_log entries keep         |
| log              |     | timestamps and action types   |
|                  |     | but user_id → [REDACTED]      |
+------------------+     +--------------------------------+
         |
         v
+------------------+
| Notify user      |  ← Email confirmation of anonymization
+------------------+
```

### Backup Window
- Anonymization reflected in next backup cycle (within 30-day backup retention)
- Backup media encrypted; key revocation ensures anonymized data unreadable after retention

---

## 6. 10-Year Retention

### Legal Basis
- Slovenian Healthcare Act: patient health records retained for minimum 10 years
- GDPR Article 5(1)(c) data minimization: retained data limited to minimum necessary for retention purpose

### Retention Enforcement
```sql
-- Soft-delete column used for retention tracking
retention_until  TIMESTAMP NOT NULL DEFAULT (created_at + INTERVAL '10 years')
deleted_at      TIMESTAMP NULL  -- NULL = active, timestamp = soft-deleted

-- Cron job or scheduled task (daily):
SELECT * FROM visit
  WHERE deleted_at IS NOT NULL
    AND retention_until < NOW()
  FOR UPDATE SKIP LOCKED;

-- Physical deletion after retention period:
-- (Executed by DBA with explicit approval, logged in audit)
DELETE FROM visit WHERE retention_until < NOW() - INTERVAL '30 days';
```

### Retention Timeline
```
Day 0    → Visit completed, clinical form created
Day 1    → 10-year retention clock starts
Year 10  → Retention period expires; anonymization triggered
Year 10+ → Data anonymized but not physically deleted (patient safety)
Year 20  → Physical deletion permitted (post-anonymization cleanup)
```

### What is NOT retained for 10 years
| Data | Retention | Reason |
|------|-----------|--------|
| Analytics events | 1 year | Pseudonymized, not health data |
| Marketing consent | Until revoked | Can be deleted on withdrawal |
| Session tokens | Access token lifetime | Security |
| Refresh token hashes | Max 10 days | Security |

---

## 7. Data Flow Summary Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       DATA SUBJECT (Patient / Provider)                   │
└────────────────────────────┬───────────────────────────────────────────┘
                             │
                             │ 1. Consent Capture
                             │    POST /auth/register → auth.consent
                             v
┌──────────────────────────────────────────────────────────────────────────┐
│                     TRUSTED BOUNDARY (JWT + RBAC)                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐          │
│  │   auth/    │  │   user/   │  │  booking/  │  │   visit/   │          │
│  │            │  │            │  │            │  │            │          │
│  │ • register │  │ • profile  │  │ • create   │  │ • clinical │          │
│  │ • login    │  │ • address  │  │ • status   │  │   forms    │          │
│  │ • refresh  │  │ • documents│  │ • cancel   │  │ • vitals   │          │
│  │ • logout   │  │ • export   │  │ • list     │  │ • media    │          │
│  │ • MFA      │  │ • GDPR     │  │            │  │ • PDF      │          │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘          │
│                        │              │              │                    │
│                        v              v              v                    │
│  ┌─────────────────────────────────────────────────────────────────┐     │
│  │                   AUDIT LOG (append-only)                        │     │
│  │  user_id | action | resource_type | resource_id | timestamp     │     │
│  └─────────────────────────────────────────────────────────────────┘     │
└────────────────────────────┬───────────────────────────────────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          v                  v                  v
   ┌────────────┐     ┌────────────┐     ┌────────────┐
   │ PostgreSQL │     │   Redis    │     │    S3      │
   │ (encrypted│     │ (TLS+pwd)  │     │ (SSE-KMS)  │
   │  at rest)  │     │            │     │            │
   └────────────┘     └────────────┘     └────────────┘
          │                  │                  │
          v                  v                  v
   ┌────────────┐     ┌────────────┐     ┌────────────┐
   │  billing/  │     │ slot lock  │     │ documents  │
   │  Stripe    │     │  matching  │     │  media     │
   └────────────┘     └────────────┘     │  reports   │
                                          └────────────┘
```

---

## 8. Third-Party Data Transfers

| Processor | Data | Legal Basis | DPA |
|-----------|------|-------------|-----|
| Stripe | Billing (card, invoice) | Contract execution | Yes |
| Twilio | Phone number for SMS | Consent | Yes |
| SendGrid | Email address for notifications | Legitimate interest | Yes |
| FCM | Device tokens for push | Consent (optional) | Yes |
| APNs | Device tokens for push | Consent (optional) | Yes |
| AWS (S3, RDS, KMS) | All data | Contract (AWS DPA) | Yes |
| NIJZ | Health records (mandatory reporting) | Legal obligation | Separate agreement |
| ZZZS | Billing/insurance records | Legal obligation | Separate agreement |

---

## 9. Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-15 | Security Team | Initial GDPR data flow documentation |
