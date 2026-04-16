# Backend — Java / Spring Boot

## Stack
- Java 21 (virtual threads, records, pattern matching)
- Spring Boot 3.x (Maven modular structure)
- PostgreSQL (schema-per-module isolation — one schema per domain module)
- Redis (slot locking, caching)
- S3 (document and PDF storage)
- Elasticsearch (search — MVP optional)
- Kafka (Spring Cloud Stream — async events)
- OpenAPI 3.0 (Springdoc-openapi)

## Module Structure
Each domain module follows DDD: `domain/` → `application/` → `adapters/`

```
backend/src/main/java/com/zdravdom/
├── auth/               → domain/, application/, adapters/inbound/, adapters/out/
├── user/               → patient & provider profiles, documents, GDPR
├── booking/            → CRUD, status machine (requested→confirmed→in_progress→completed→rated)
├── matching/           → geo-query (PostGIS), scoring, slot locking
├── visit/              → clinical forms, vitals, media, e-signature, PDF generation
├── billing/            → Stripe, invoice/UPN, refunds, commission, payouts
├── notification/       → FCM/APNs, SMS (Twilio), email (SendGrid)
├── cms/                → services, packages, pricing, content, localization
├── analytics/          → events, KPIs, data warehouse
└── integration/        → nijz/, zzzs/, hospital/ (healthcare system adapters)
```

## Critical Rules

### Coding
- Prefer immutability (records, `final` fields)
- Use Lombok sparingly — `@Getter`/`@ToString` instead of `@Data`
- Handle exceptions with global `@ControllerAdvice`
- No Spring annotations in domain entities (keep domain framework-agnostic)
- Follow SOLID strictly

### Database
- One PostgreSQL schema per module (e.g., `auth`, `user`, `booking`, `visit`, `billing`)
- Use `application.yml` for all configuration — no hardcoded values
- Schema migrations via Flyway
- Use JPA/Hibernate but be aware of N+1 query issues

### API
- OpenAPI contract first — code must match spec, not vice versa
- JWT Bearer tokens with RBAC roles: PATIENT, PROVIDER, OPERATOR, ADMIN, SUPERADMIN
- Standard error responses: 400 (validation), 401 (unauthenticated), 403 (forbidden), 404 (not found), 500 (server error)
- All endpoints must have unit tests

### Security
- JWT auth with short-lived access tokens + refresh tokens
- RBAC enforced at controller layer
- Audit logging for all health data access (immutable log)
- Encryption at rest for clinical documents in S3
- No secrets in code — environment variables only

### Testing
- Unit: JUnit 5 + AssertJ + Mockito (>90% coverage on domain)
- Integration: Testcontainers (PostgreSQL, Redis, Kafka)
- Load testing deferred to Phase 3 (k6/Gatling)

### Resilience
- Resilience4j circuit breakers for external integrations (Stripe, NIJZ, ZZZS)
- Retry with exponential backoff for transient failures
- Redis slot locking uses TTL — test concurrent booking scenarios

## Active Skills
- java: Spring Boot patterns, Maven conventions
- ddd: domain-driven design, bounded contexts, aggregates
- api-development: REST, OpenAPI contract-first
- security: JWT, RBAC, audit logging, OWASP
- containerized: Docker, multi-stage builds
- cicd: GitHub Actions pipelines for Maven projects
- compliance: GDPR health data, 10-year retention, eIDAS signatures
- scalability: Redis slot locking, caching strategies, PostgreSQL tuning

## Key Files
- `pom.xml` — Maven build with all dependencies
- `src/main/resources/application.yml` — Spring configuration
- `src/main/java/com/zdravdom/ZdravdomApplication.java` — entry point
- `api-contract/openapi.yaml` — API contract (source of truth)
- `docs/adr/` — Architecture Decision Records

## Risk Factors Specific to Backend
- Slot locking race conditions (test concurrent booking with Redis locks)
- PDF generation async timeout (use queue, not synchronous HTTP)
- Stripe webhook idempotency (store processed event flags in DB)
- NIJZ/ZZZS integration deferred to Phase 2/4 — MVP uses manual workarounds
- Only one person knows full infrastructure — rotate DevOps tasks in Phase 1

## See Also
- Root CLAUDE.md for project overview and commercial context
- `/shared/CLAUDE.md` for shared TypeScript packages consumed by frontend apps