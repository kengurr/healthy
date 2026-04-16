# Zdravdom — Project Overview

## What Is This Project?
**Zdravdom** is a home healthcare platform connecting patients and families with verified healthcare providers (nurses, physiotherapists, doctors, etc.) who deliver care at the patient's home. Primary market is Slovenia, with future EU expansion.

Platform components:
- **Patient App** — iOS + Android (React Native) + responsive web
- **Provider App** — iOS + Android (React Native) + web dashboard
- **Admin Portal** — React web application (browser only)
- **Backend** — Java 21 / Spring Boot 3 modular monolith
- **Shared** — TypeScript packages consumed by all frontend apps

## Commercial Summary
- **Main contract**: EUR 160,000 (Phases 0–3, 39 weeks / ~9 months)
  - Phase 0: Discovery (3 weeks, EUR 8,000)
  - Phase 1: MVP Build (20 weeks, EUR 80,000)
  - Phase 2: Operational Scale (12 weeks, EUR 48,000)
  - Phase 3: Hardening, Security & Launch (4 weeks, EUR 24,000)
- **Conditional add-ons**: Phase 4 Healthcare Integration (EUR 16,000), UX/UI Design (EUR 12,000), External Pentest (EUR 6,000 pass-through)
- **Team**: 4 developers (1 Tech Lead + 3 Senior/Medior Java+DevOps)

## Architecture Decision Record

### Backend: Modular Monolith (NOT microservices)
For a 4-dev team and 9-11 month timeline, microservices would create too much operational overhead. We use modular monolith with enforced module boundaries:
- Schema-per-module in PostgreSQL (module isolation)
- Spring Events for cross-module communication
- ArchUnit tests to enforce package boundaries
- Extracted to separate services only when team size justifies it (target: 15+ devs)

### Frontend: React Native (mobile) + React (admin web)
- Patient App: React Native (Expo) — iOS + Android + responsive web
- Provider App: React Native (Expo) — iOS + Android + web dashboard
- Admin Portal: React (Vite) — browser only
- Shared TypeScript packages: api-client, types, hooks, constants

### Tech Stack
- **Backend**: Java 21, Spring Boot 3.x, Maven, PostgreSQL, Redis, S3, Elasticsearch
- **Messaging**: Kafka (Spring Cloud Stream)
- **API**: OpenAPI 3.0 / Swagger (Springdoc-openapi)
- **Security**: JWT auth, RBAC, OAuth2
- **Infrastructure**: AWS ECS Fargate or Hetzner (GDPR-compliant EU region)
- **CI/CD**: GitHub Actions
- **Monitoring**: Grafana, Prometheus, Sentry

### Module Structure (Backend)
```
com.zdravdom/
├── auth/           → Registration, login, MFA, JWT refresh, RBAC
├── user/           → Patient/provider profiles, documents, verification, GDPR
├── booking/        → Booking CRUD, status machine, cancellations
├── matching/       → Geo-query, scoring, Redis slot locking
├── visit/          → Clinical forms, vitals, media, signatures, PDF reports
├── billing/        → Stripe, invoice/UPN, refunds, commission, payouts
├── notification/   → FCM/APNs, SMS (Twilio), email (SendGrid)
├── cms/            → Services, packages, pricing, content, localization
├── analytics/      → Events, KPIs, data warehouse
└── integration/    → NIJZ, ZZZS, hospital EHR, supplier APIs
```

## Phase Timeline
- **Phase 0** (Weeks 1-3): Discovery workshops, architecture, backlog, infrastructure plan
- **Phase 1** (Weeks 4-23): MVP build — all services, REST APIs, CI/CD, staging, monitoring, security
- **Phase 2** (Weeks 24-35): Automation, CMS maturity, BIRPIS/ZZZS API, supplier APIs, analytics
- **Phase 3** (Weeks 36-39): Load testing, pentest, GDPR audit, production hardening, go-live

## Known Risk Factors
These must be considered in every architectural decision:
1. **Slot locking race conditions** — Redis distributed locking for concurrent booking. Double-booking risk if TTL misconfigured. Implement idempotency keys at API layer.
2. **PDF generation timeouts** — Large clinical documents. Use async queue (Spring @Async + Redis), max size limits, S3 caching.
3. **GPS tracking on iOS** — Background location requires special entitlement. Apple may reject if justification insufficient. Opt-in only.
4. **E-signature legal validity** — eIDAS regulation. Clarify required signature class (simple consent vs qualified). Use third-party (HelloSign/DocuSign) if qualified needed.
5. **OCR accuracy** — Never 100%. Manual review workflow required. OCR accuracy target: 85-90%. Low-confidence fields flagged for human review.
6. **Calendar sync complexity** — Bidirectional vs read-only are vastly different efforts. MVP: read-only only. Phase 2: bidirectional with clear scope.
7. **Hospital EHR integration** — "One of the largest integration risks." External system availability outside our control. Manual PDF fallback in MVP.
8. **Infrastructure knowledge bottleneck** — Only one person knows full infra. All infrastructure as code (Terraform/Pulumi) with runbooks. Rotate DevOps tasks.
9. **Stripe webhook idempotency** — Stripe may deliver webhooks multiple times. Use idempotency key, store processed flag in DB.
10. **10-year health data retention** — Soft-delete with anonymization (never hard-delete). Retention enforced at DB level. Legal review of anonymization approach.

## Project Subfolders
| Folder | Purpose |
|--------|---------|
| `/backend` | Java/Spring Boot application (Maven project) |
| `/shared` | TypeScript packages (npm workspace) |
| `/patient-app` | React Native (Expo) mobile app |
| `/provider-app` | React Native (Expo) mobile app |
| `/admin-portal` | React (Vite) web application |
| `/docs` | Commercial proposal, UI/UX spec, scope documents |

## Team Structure
| Role | Count | Responsibility |
|---|---|---|
| Tech Lead | 1 | Architecture, API design, code review, DevOps |
| Senior Java Dev | 1 | Auth, booking, billing, integration modules |
| Java Developer | 1 | Visit, CMS, notification modules |
| Java/DevOps | 1 | Infrastructure, CI/CD, monitoring, security |

## Active Skills (VoltAgent agents for this project)
- small-team: lightweight coordination, pair programming, no formal Agile ceremonies
- cicd: GitHub Actions across all services

## Key Rules
- **API contract is source of truth**: OpenAPI spec in `/backend/api-contract/openapi.yaml`
- Breaking API changes require formal change request + version bump
- All architectural decisions must be logged in `/docs/adr/` (Architecture Decision Records)
- No secrets in code — use environment variables or HashiCorp Vault
- GDPR from Day 1 — encryption at rest, audit logging, consent management, 10-year retention
- Each subfolder has its own CLAUDE.md with team-specific rules — read both root and subfolder CLAUDE.md when working in any folder