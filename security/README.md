# Zdravdom Security Baseline

## Overview
This document defines the security requirements and controls for the Zdravdom healthcare platform. It covers OWASP mitigations, JWT implementation, RBAC enforcement, GDPR compliance, and encryption requirements.

**Applies to**: All platform components (Backend, Patient App, Provider App, Admin Portal, Shared packages)

---

## 1. OWASP Top 10 Mitigations (A01-A10)

### A01 — Broken Access Control
- [ ] All endpoints enforce JWT Bearer authentication; no public endpoints except `/auth/login`, `/auth/register`, `/actuator/health`
- [ ] RBAC enforced at controller layer via `@PreAuthorize("hasRole('ROLE_PATIENT')")` etc.
- [ ] Row-level security: patients can only access their own records; providers only their assigned visits
- [ ] Admin portal routes protected; SUPERADMIN/ADMIN roles required for user management
- [ ] Direct object reference (IDOR) prevented by scoping queries to current user context
- [ ] CORS restricted to known origins only (patient app, provider app, admin portal domains)
- [ ] No directory listing or insecure static file serving

### A02 — Cryptographic Failures
- [ ] TLS 1.2+ for all connections (inbound and outbound); TLS 1.0/1.1 disabled
- [ ] PostgreSQL connection uses SSL mode `require` or `verify-full`
- [ ] Redis connection uses TLS or password authentication (no plaintext)
- [ ] S3 buckets use SSE-KMS encryption (AWS managed or CMK)
- [ ] Clinical documents and media in S3 encrypted at rest with SSE-S3 or SSE-KMS
- [ ] No sensitive data (passwords, tokens, health records) in URL query strings or logs
- [ ] Secrets injected via environment variables; never hardcoded in code
- [ ] Password hashing: bcrypt with cost factor >= 12; no MD5/SHA-1 for passwords

### A03 — Injection
- [ ] Parameterized queries via JPA/Hibernate; no raw SQL concatenation
- [ ] Input validation on all REST endpoints (Bean Validation `@Valid`)
- [ ] Sanitization of user-supplied content rendered in web views (if any)
- [ ] Flyway migrations reviewed; no dynamic SQL in migrations
- [ ] Kafka deserializers use String or trusted type; no unsafe object deserialization

### A04 — Insecure Design
- [ ] Threat modelling conducted during Phase 0 (STRIDE)
- [ ] Booking slot locking uses Redis with TTL to prevent double-booking
- [ ] Idempotency keys on Stripe webhook processing and all state-changing API calls
- [ ] Rate limiting on auth endpoints: max 5 failed login attempts per 15 minutes per IP
- [ ] Account lockout after repeated failed MFA attempts (max 10)
- [ ] Fuzzy matching for patient data queries to prevent timing attacks

### A05 — Security Misconfiguration
- [ ] Spring profiles: `dev`, `staging`, `prod` — no secrets in non-production profiles
- [ ] Actuator endpoints exposed only to `/actuator/health` publicly; all others require authentication
- [ ] `show-sql: false` in production; `hibernate.generate_schema` disabled
- [ ] HTTP headers: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Strict-Transport-Security`, `Content-Security-Policy`
- [ ] Error responses return generic messages; stack traces only in non-prod
- [ ] Unused Spring Boot starters and sample data excluded from production build

### A06 — Vulnerable and Outdated Components
- [ ] Dependency scan via Trivy on every CI run (CRITICAL/HIGH failures block merge)
- [ ] `npm audit` / `yarn audit` enforced in frontend CI pipelines
- [ ] No deprecated dependencies (check before upgrading major versions)
- [ ] Base Docker images pinned to specific digest (not `:latest`)
- [ ] Maven dependency check: `mvn dependency:analyze` in CI
- [ ] Third-party libraries reviewed; no unknown/unmaintained sources

### A07 — Identification and Authentication Failures
- [ ] MFA enforced for PROVIDER, OPERATOR, ADMIN, SUPERADMIN roles (TOTP via authenticator app)
- [ ] MFA optional but encouraged for PATIENT role (SMS or TOTP)
- [ ] JWT access token expiry: 15 minutes (configurable via `JWT_ACCESS_TOKEN_EXPIRY`)
- [ ] JWT refresh token expiry: 7 days dev/staging, 10 days production
- [ ] Refresh token stored as hash (SHA-256) in DB; original never stored
- [ ] Auth endpoints rate-limited (see A04)
- [ ] JWT refresh token rotation: new refresh token issued on each `/auth/refresh`
- [ ] Logout invalidates refresh token server-side (token hash removed from DB)
- [ ] Session invalidation on password change

### A08 — Software and Data Integrity Failures
- [ ] CI pipeline is the only path to deployment (no direct server modifications)
- [ ] Docker image built from locked Maven/npm dependency trees
- [ ] Trivy scan of Docker image before push; CRITICAL vulnerabilities block deployment
- [ ] GitHub Actions workflows signed; no `pull_request_target` with write access
- [ ] Third-party CI actions pinned to version SHA (not `@v4` floating tags)
- [ ] No build artifacts from untrusted sources

### A09 — Security Logging and Monitoring
- [ ] Audit log for all health data access (immutable append-only table `audit_log`)
- [ ] Audit log fields: `user_id`, `action`, `resource_type`, `resource_id`, `timestamp`, `ip_address`, `user_agent`
- [ ] Login success/failure logged with IP and user agent
- [ ] RBAC denials logged (403 responses)
- [ ] Sentry error monitoring with stack traces in staging only; generic errors in prod
- [ ] Prometheus metrics: login failures, auth denials, API latency p99
- [ ] Alerting: >10 failed logins/minute → notify on-call; >5 403/minute → investigate
- [ ] GDPR audit trail retained for 10 years

### A10 — Server-Side Request Forgery (SSRF)
- [ ] External API calls (NIJZ, ZZZS, Stripe, Twilio, SendGrid) use allowlists for hostnames
- [ ] No user-supplied URL passed to HTTP client without validation
- [ ] AWS S3 endpoint validated against known bucket names (no arbitrary bucket access)
- [ ] Redis/S3 connections restricted to configured infrastructure; no cross-tenant access

---

## 2. JWT Implementation Checklist

### Algorithm
- [ ] Algorithm: **RS256** (asymmetric) preferred; HS256 acceptable only for stateless microservices with shared secret
- [ ] For Zdravdom monolith: RS256 with RSA 2048-bit key pair rotated annually
- [ ] `alg` header explicitly validated — reject `none`, `HS384`, `HS512` variants
- [ ] Public key exposed at `/auth/.well-known/jwks.json`

### Token Structure
```
Header:  { "alg": "RS256", "typ": "JWT" }
Payload: { "sub": "<user_id>", "role": "ROLE_PATIENT",
           "iat": <issued_at>, "exp": <expires_at>,
           "jti": "<unique_id>", "refreshHash": "<hash>" }
```

### Expiry
- [ ] Access token: 15 minutes (`JWT_ACCESS_TOKEN_EXPIRY=900000`)
- [ ] Refresh token: 7 days staging, 10 days production (`JWT_REFRESH_TOKEN_EXPIRY`)
- [ ] `exp` claim validated on every request; reject expired tokens with 401
- [ ] `iat` claim validated; reject tokens with future `iat` (clock skew tolerance: 60 seconds)

### Refresh Flow
- [ ] Refresh token sent as `HttpOnly` cookie OR in request body (never URL)
- [ ] Refresh token hashed (SHA-256) in DB; original never stored or logged
- [ ] Refresh token rotation: every `/auth/refresh` issues new access + new refresh token
- [ ] Old refresh token immediately invalidated after rotation (use-and-rotate)
- [ ] Refresh token reuse detection: if a hash is seen twice, revoke entire token family
- [ ] Refresh tokens invalidated on password change, account lockout, or explicit logout

### Storage
- [ ] Access token: React Native `SecureStore` (iOS Keychain / Android Keystore); never AsyncStorage or localStorage
- [ ] Access token: included in `Authorization: Bearer <token>` header only
- [ ] Refresh token: `HttpOnly` cookie with `SameSite=Strict`; not accessible from JavaScript
- [ ] Tokens never logged or written to console
- [ ] Tokens cleared on app uninstall / device wipe

### Validation
- [ ] Signature verified against JWKS from `/auth/.well-known/jwks.json`
- [ ] `iss` (issuer) validated against configured base URL
- [ ] `aud` (audience) validated (must include `zdravdom-api`)
- [ ] `jti` checked against revocation list (Redis SET with TTL matching token expiry)
- [ ] Token claims not used as security decision without full validation

---

## 3. RBAC Enforcement Checklist

### Roles
| Role | Description |
|------|-------------|
| PATIENT | Patient or family member |
| PROVIDER | Verified healthcare provider (nurse, physiotherapist, doctor) |
| OPERATOR | Zdravdom operations staff |
| ADMIN | Platform administrator |
| SUPERADMIN | Super administrator (full access) |

### Implementation
- [ ] Roles stored in `auth.role` table; many-to-many with `auth.user`
- [ ] JWT payload includes `role` claim; role loaded from DB on token refresh (not cached longer than access token lifetime)
- [ ] All REST controllers protected with `@PreAuthorize` annotations
- [ ] Method-level security: `@PreAuthorize("hasRole('PROVIDER') and #ownerId == authentication.principal.id")`
- [ ] Admin routes in Admin Portal check SUPERADMIN/ADMIN role server-side (not just frontend)

### Endpoint Protection
- [ ] `/auth/**` — public (login, register, refresh)
- [ ] `/booking/**` — PATIENT (own), PROVIDER (assigned), OPERATOR, ADMIN
- [ ] `/visit/**` — PROVIDER (own), OPERATOR, ADMIN
- [ ] `/billing/**` — PATIENT (own invoices), PROVIDER (own payouts), ADMIN, SUPERADMIN
- [ ] `/user/**` — PATIENT (own profile), PROVIDER (own profile), OPERATOR (any patient), ADMIN (any user)
- [ ] `/admin/**` — ADMIN, SUPERADMIN only
- [ ] `/actuator/health` — public
- [ ] `/actuator/**` — authenticated, non-public

### Audit
- [ ] Every RBAC denial (403) logged with user ID, attempted action, resource
- [ ] Privilege escalation attempts (e.g., PATIENT accessing ADMIN endpoints) trigger alert

---

## 4. GDPR Compliance Checklist

### Consent
- [ ] Consent captured at registration via explicit opt-in checkbox (not pre-ticked)
- [ ] Consent text in Slovenian and English; clear, plain language
- [ ] Consent record stored: `user_id`, `consent_type`, `version`, `timestamp`, `ip_address`, `user_agent`
- [ ] Consent version tracked; re-prompt if privacy policy changes
- [ ] Right to withdraw consent: user can revoke at any time via `/user/settings/consent`
- [ ] Withdrawal does not affect lawfulness of processing before withdrawal

### Data Retention
- [ ] **10-year retention** for health records (clinical forms, visit notes, vitals, media, signatures)
- [ ] Retention enforced at database level via `deleted_at` soft-delete + `retention_until` column
- [ ] Soft-delete: records never physically deleted until retention period expires
- [ ] Deleted account anonymization: PII (name, email, phone, address) replaced with `[REDACTED]`; health data retained
- [ ] Audit logs retained for 10 years (legal obligation)

### Right of Access
- [ ] User can request full data export via `/user/settings/export` (PDF + JSON)
- [ ] Export fulfilled within 30 days (automated where possible)
- [ ] Export includes: profile, bookings, visits, clinical forms, invoices, consent records, audit log entries

### Right to Erasure (Art. 17)
- [ ] "Right to be forgotten" fulfilled via anonymization, not physical deletion (health data)
- [ ] Anonymization: replace all PII with `[REDACTED_<timestamp>]`
- [ ] Erasure request logged in audit log with requestor ID, date, scope
- [ ] Backup data anonymized within 30-day backup retention window

### Data Controller / Processor
- [ ] Data Processing Agreement (DPA) in place with all third-party processors (Stripe, Twilio, SendGrid, AWS, FCM)
- [ ] NIJZ/ZZZS integration: separate DPA required; health data transferred only under legal basis
- [ ] Sub-processors list maintained and available on request

### Data Protection Impact Assessment (DPIA)
- [ ] DPIA conducted during Phase 0; reviewed before Phase 3 launch
- [ ] DPIA covers: booking, clinical forms, visit notes, billing, NIJZ/ZZZS integration

### Privacy by Design
- [ ] Data minimization: only data necessary for each purpose collected
- [ ] Pseudonymization where possible (e.g., analytics uses pseudonymous user IDs)
- [ ] Encryption at rest for all health data
- [ ] No health data in logs; PII in logs minimized

---

## 5. Encryption Requirements

### In Transit
| Connection | Minimum TLS |
|-----------|-------------|
| Client → Backend API | TLS 1.2+ |
| Backend → PostgreSQL | TLS 1.2+ (SSL mode `require` or `verify-full`) |
| Backend → Redis | TLS 1.2+ or password auth |
| Backend → S3 | TLS 1.2+ (HTTPS) |
| Backend → External APIs (Stripe, Twilio, SendGrid, NIJZ, ZZZS) | TLS 1.2+ |
| Mobile App → Backend | TLS 1.2+; certificate pinning recommended |

### At Rest
| Data Type | Encryption |
|-----------|-----------|
| PostgreSQL data files | Transparent Data Encryption (TDE) or filesystem-level (AWS RDS/Cloud disk encryption) |
| S3 clinical documents | SSE-KMS with CMK (customer-managed key) |
| S3 visit media | SSE-KMS with CMK |
| S3 reports/PDFs | SSE-KMS with CMK |
| Redis persistence (RDB) | Redis 7+ with AES-256 or filesystem-level encryption |
| Backup snapshots | Encrypted (AWS EBS encryption or equivalent) |
| JWT signing keys | RSA private key stored in AWS Secrets Manager or HashiCorp Vault |

### Key Management
- [ ] Encryption keys managed via AWS KMS (CMK per environment) or HashiCorp Vault
- [ ] KMS/CMK rotation: annual automatic rotation enabled
- [ ] Secrets never in code, never in Docker images, never in GitHub Actions logs
- [ ] Secrets injected at runtime via environment variables or secret injection sidecar
- [ ] Emergency key revocation procedure documented and tested
- [ ] Key access logged (AWS CloudTrail / Vault audit logs)

---

## 6. Related Documents

- `backend/api-contract/openapi.yaml` — API contract (source of truth)
- `backend/src/main/resources/application-prod.yml` — Production Spring configuration
- `backend/src/main/resources/application.yml` — Base Spring configuration
- `docs/adr/` — Architecture Decision Records
- `.github/workflows/ci.yml` — CI/CD pipeline (includes Trivy scanning)
- `security/secrets-check.yml` — GitHub Actions secrets detection workflow
- `.env.example` — Environment variable template

---

## 7. Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-15 | Security Team | Initial baseline |
