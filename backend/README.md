# Zdravdom Backend

Home healthcare platform backend - Java 21 / Spring Boot 3 modular monolith.

## Architecture Overview

The backend follows Domain-Driven Design (DDD) with a modular monolith architecture:

```
com.zdravdom/
├── auth/           → Authentication, JWT, RBAC
├── user/           → Patient/Provider profiles, documents, GDPR
├── booking/        → Appointment scheduling, status machine
├── matching/       → Geo-query, scoring, Redis slot locking
├── visit/          → Clinical documentation, vitals, escalations
├── billing/        → Stripe payments, invoices, commissions
├── notification/   → Push notifications, SMS, email
├── cms/            → Services, packages, pricing, content
├── analytics/      → Events, KPIs, data warehouse
└── integration/    → NIJZ, ZZZS, hospital EHR integrations
```

### Module Structure (DDD Layers)

Each module follows the pattern: `domain/` → `application/` → `adapters/`

```
module/
├── domain/          → Entities, value objects, domain events (framework-agnostic)
├── application/     → Use cases, application services, DTOs
└── adapters/
    ├── inbound/     → REST controllers, web controllers
    └── out/         → Repositories, external service clients
```

## Tech Stack

- **Java 21** with virtual threads, records, pattern matching
- **Spring Boot 3.3** with Spring Security
- **PostgreSQL** with schema-per-module isolation
- **Redis** for slot locking and caching
- **S3** for document storage
- **Kafka** for async events
- **Resilience4j** for circuit breakers
- **Flyway** for database migrations
- **OpenAPI 3.0** / Springdoc

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
- Docker (optional, for local infrastructure)

### Environment Setup

1. **Clone and setup environment variables:**

```bash
# Create config/application-local.yml or set environment variables
export DATABASE_HOST=localhost
export DATABASE_PORT=5432
export DATABASE_NAME=zdravdom_dev
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your-256-bit-secret-key-minimum-32-characters
```

2. **Run infrastructure (Docker Compose):**

```bash
docker-compose up -d
```

3. **Build and run:**

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/zdravdom-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# Or with Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Profiles

- `dev` - Local development with relaxed settings
- `staging` - Pre-production environment
- `prod` - Production with strict settings

### Database Migrations

Migrations are managed by Flyway and run automatically on startup.

```bash
# Run migrations manually
mvn flyway:migrate

# Clean and re-migrate (dev only!)
mvn flyway:clean flyway:migrate
```

## Module Details

### Auth Module
- User registration and login
- JWT access/refresh tokens
- MFA support
- RBAC with roles: PATIENT, PROVIDER, OPERATOR, ADMIN, SUPERADMIN

### User Module
- Patient profiles with health information
- Provider profiles with specializations
- Address management
- Document uploads (ID, insurance, referrals)

### Booking Module
- Appointment scheduling
- Status machine: REQUESTED → CONFIRMED → IN_PROGRESS → COMPLETED
- Cancellation handling
- Idempotency keys for safe retries

### Visit Module
- Clinical documentation
- Vitals recording
- Escalation handling for emergencies
- Visit reports

### CMS Module
- Service definitions
- Service packages (S/M/L)
- Pricing management

### Matching Module
- Provider discovery
- Slot locking (Redis) for concurrent booking prevention
- Geo-based matching

## Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify -Pintegration

# ArchUnit architecture tests
mvn test -Dtest=ArchTest
```

## API Documentation

Swagger UI available at: `http://localhost:8080/api/v1/swagger-ui.html`

OpenAPI spec: `src/main/resources/openapi.yaml`

## Security

- JWT Bearer authentication
- RBAC enforced at controller level
- Audit logging for health data access (GDPR)
- Encryption at rest for clinical documents in S3

## Monitoring

Actuator endpoints:
- Health: `/api/v1/actuator/health`
- Metrics: `/api/v1/actuator/metrics`
- Prometheus: `/api/v1/actuator/prometheus`

## Project Structure

```
backend/
├── src/main/java/com/zdravdom/
│   ├── ZdravdomApplication.java
│   ├── auth/
│   ├── user/
│   ├── booking/
│   ├── visit/
│   ├── cms/
│   ├── matching/
│   ├── billing/
│   ├── notification/
│   ├── analytics/
│   ├── integration/
│   └── global/
│       └── exception/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-staging.yml
│   ├── application-prod.yml
│   └── db/migration/
├── src/test/java/
├── api-contract/
├── docs/
├── Dockerfile
└── docker-compose.yml
```

## Deployment

### Docker

```bash
# Build image
docker build -t zdravdom-backend:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_HOST=prod-db \
  zdravdom-backend:latest
```

### Kubernetes

Helm charts available in `/deploy/kubernetes/`.

## Contributing

1. Follow DDD principles - domain layer must be framework-agnostic
2. Use records for immutable value objects
3. Prefer composition over inheritance
4. All endpoints require unit tests
5. Run ArchTest before committing to enforce architecture

## License

Proprietary - All rights reserved
