# Zdravdom

Home healthcare platform connecting patients and families with verified healthcare providers (nurses, physiotherapists, doctors) who deliver care at the patient's home. Primary market is Slovenia, with future EU expansion.

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Backend](#backend)
- [Admin Portal](#admin-portal)
- [Shared Packages](#shared-packages)
- [Project Structure](#project-structure)
- [Development](#development)
- [API Documentation](#api-documentation)
- [Environment Variables](#environment-variables)

---

## Project Overview

**Platform Components:**

| Component | Technology | Description |
|-----------|------------|-------------|
| Backend | Java 21 / Spring Boot 3 | Modular monolith with DDD architecture |
| Patient App | React Native (Expo) | iOS + Android + responsive web |
| Provider App | React Native (Expo) | iOS + Android + web dashboard |
| Admin Portal | React (Vite) | Browser-only operations dashboard |
| Shared | TypeScript | API client, types, hooks, constants |

**Commercial Context:**
- Main contract: EUR 160,000 (Phases 0–3, ~9 months)
- Team: 4 developers

---

## Architecture

### Backend: Modular Monolith (DDD)

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

Each module follows DDD layers: `domain/` → `application/` → `adapters/`

### Frontend: React Native + React Web

- **Patient App**: React Native (Expo) — consumer mobile app
- **Provider App**: React Native (Expo) — provider mobile app + web dashboard
- **Admin Portal**: React (Vite) — operations/back-office web app
- **Shared**: TypeScript npm workspace consumed by all frontend apps

### Infrastructure

- PostgreSQL (schema-per-module isolation)
- Redis (slot locking, caching)
- S3 (document storage)
- Kafka (async event streaming)
- Docker Compose for local development

---

## Quick Start

### Prerequisites

- **Java 21+** (for backend)
- **Maven 3.9+** (for backend)
- **Node.js 20+** (for frontend)
- **Docker & Docker Compose** (for infrastructure)

### 1. Clone and Setup Environment

```bash
git clone <repository-url>
cd zdravdom

# Copy environment template
cp .env.example .env
```

### 2. Start Infrastructure (Docker)

```bash
cd backend
docker-compose up -d
```

This starts PostgreSQL, Redis, Kafka, LocalStack (S3 mock), and monitoring tools.

### 3. Run Backend

```bash
cd backend

# Build
mvn clean package -DskipTests

# Run
java -jar target/zdravdom-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# Or with Maven (development mode)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend runs at: **http://localhost:8080/api/v1**

### 4. Run Admin Portal

```bash
# From project root
npm install
npm run dev
```

Admin Portal runs at: **http://localhost:5173**

---

## Backend

### Tech Stack

- Java 21 with virtual threads, records, pattern matching
- Spring Boot 3.3 with Spring Security
- PostgreSQL 15+ with Flyway migrations
- Redis 7+ for slot locking and caching
- S3 for document storage
- Kafka for async events
- OpenAPI 3.0 / Springdoc for API documentation

### Module Structure

```
backend/src/main/java/com/zdravdom/
├── auth/               → domain/, application/, adapters/
├── user/               → patient & provider profiles, documents
├── booking/            → CRUD, status machine
├── matching/           → geo-query, scoring, slot locking
├── visit/              → clinical forms, vitals, escalations
├── billing/            → Stripe integration, invoices
├── notification/       → FCM/APNs, SMS, email
├── cms/                → services, packages, pricing
├── analytics/          → events, KPIs
├── integration/        → NIJZ, ZZZS, hospital adapters
└── global/             → exception handling
```

### Database Migrations

Migrations run automatically on startup via Flyway. To run manually:

```bash
mvn flyway:migrate

# Clean and re-migrate (dev only!)
mvn flyway:clean flyway:migrate
```

### Profiles

| Profile | Use Case |
|---------|----------|
| `dev` | Local development |
| `staging` | Pre-production |
| `prod` | Production |

### Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify -Pintegration

# Architecture tests
mvn test -Dtest=ArchTest
```

### Monitoring

Actuator endpoints:
- Health: `GET /api/v1/actuator/health`
- Metrics: `GET /api/v1/actuator/metrics`
- Prometheus: `GET /api/v1/actuator/prometheus`

---

## Admin Portal

### Tech Stack

- React 19 (Vite)
- TypeScript (strict mode)
- React Router v7
- TanStack Query (server state)
- shadcn/ui (Radix UI based components)
- Recharts (analytics charts)

### Key Screens

- **A-01**: Admin Login
- **A-02**: Operations Dashboard (KPIs, stats)
- **A-03**: Provider Verification Queue
- **A-04**: Booking Management & Dispatch
- **A-05**: Service & Pricing CMS
- **A-06**: Analytics Dashboard
- **A-07**: Support Tools
- **A-08**: GDPR & Compliance
- **A-09**: Escalations Queue

### RBAC Roles

| Role | Access |
|------|--------|
| OPERATOR | Booking queue, provider management, support |
| ADMIN | Everything + user management, CMS, analytics |
| SUPERADMIN | Everything + system configuration, audit logs |

### Available Scripts

```bash
cd admin-portal

npm run dev          # Start development server
npm run build        # Production build
npm run lint         # Lint code
npm run preview      # Preview production build
```

---

## Shared Packages

TypeScript packages consumed by all frontend apps.

### Packages

| Package | Purpose |
|---------|---------|
| `@zdravdom/api-client` | Auto-generated from OpenAPI spec |
| `@zdravdom/types` | Shared TypeScript interfaces |

### Building Shared Packages

```bash
# From project root
npm run build --workspaces
```

### Regenerating API Client

When the OpenAPI spec changes, regenerate the client:

```bash
cd shared/packages/api-client
# Run generation script (see package.json)
```

---

## Project Structure

```
zdravdom/
├── backend/              # Java/Spring Boot application (Maven)
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── api-contract/     # OpenAPI specification
│   ├── docker-compose.yml
│   └── pom.xml
├── admin-portal/         # React (Vite) admin web app
│   ├── src/
│   └── package.json
├── shared/               # TypeScript packages (npm workspace)
│   ├── packages/
│   │   ├── api-client/
│   │   └── types/
│   └── package.json
├── patient-app/          # React Native (Expo) patient app
├── provider-app/        # React Native (Expo) provider app
├── infrastructure/      # Terraform/Pulumi IaC
├── security/            # Security policies and audits
├── docs/                # Architecture Decision Records, specs
├── design/              # UI/UX mockups and assets
├── CLAUDE.md            # Project-level instructions
└── README.md            # This file
```

---

## Development

### Environment Variables

Copy `.env.example` to `.env` in each subfolder and configure:

**Backend required:**
- `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `JWT_SECRET` (minimum 32 characters)

**Optional for full feature set:**
- Stripe keys for payment testing
- Firebase Cloud Messaging for push notifications
- Twilio for SMS
- SendGrid for email

### Code Conventions

**Backend:**
- Domain layer must be framework-agnostic
- Use records for immutable value objects
- All endpoints require unit tests
- Run ArchTest before committing

**Frontend:**
- Strict TypeScript
- Use shared packages, not app-specific types
- API client is auto-generated — do not edit manually

### API Contract

OpenAPI spec is the source of truth: `backend/api-contract/openapi.yaml`

Breaking changes require formal change request + version bump.

---

## API Documentation

When backend is running, access Swagger UI:
**http://localhost:8080/api/v1/swagger-ui.html**

OpenAPI spec: `backend/api-contract/openapi.yaml`

### Authentication

JWT Bearer token required in `Authorization` header:

```
Authorization: Bearer <access_token>
```

Roles: `PATIENT`, `PROVIDER`, `OPERATOR`, `ADMIN`, `SUPERADMIN`

### Standard Error Responses

| Code | Meaning |
|------|---------|
| 400 | Validation error |
| 401 | Unauthenticated |
| 403 | Forbidden (RBAC) |
| 404 | Not found |
| 500 | Server error |

---

## Troubleshooting

### Database Connection Issues

Ensure PostgreSQL is running:
```bash
docker-compose ps postgres
```

Check connection:
```bash
psql -h localhost -U postgres -d zdravdom
```

### Redis Connection Issues

Check Redis is running:
```bash
docker-compose ps redis
redis-cli ping
```

### Frontend Build Issues

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Backend Build Issues

```bash
# Clean Maven cache
mvn clean
rm -rf ~/.m2/repository/com/zdravdom
```