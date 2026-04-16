# Zdravdom Database Schema vs OpenAPI Alignment

## Overview

This document maps the OpenAPI `openapi.yaml` schemas to PostgreSQL tables,
identifies gaps, and tracks migrations needed to align the DB with the API contract.

---

## Schema Map

### `auth` schema ✓ Complete

| OpenAPI Schema | DB Table | Status |
|---|---|---|
| User (implicit) | `auth.users` | ✓ Mapped |
| AuthTokens | `auth.auth_tokens` | ✓ Mapped |
| Role enum | `auth.users.role` | ✓ PENDING → OPERATOR missing from enum |

**Note:** `auth.users` role column stores strings directly. Roles are:
PATIENT, PROVIDER, OPERATOR, ADMIN, SUPERADMIN

---

### `user` schema — Mostly Complete

| OpenAPI Schema | DB Table | Status |
|---|---|---|
| Patient | `user.patients` | ✓ Mapped |
| Address | `user.addresses` | ✓ Mapped (label→col: add in V4) |
| Provider | `user.providers` | ✓ Mapped |
| PatientDocument | `user.patient_documents` | ✓ Mapped |
| ProviderAvailability | `user.provider_availability` | ✓ Added in V3 |
| WeeklyScheduleItem | `user.provider_availability` | ✓ Per-row-per-day |
| BlockedDates | `user.provider_blocked_dates` | ✓ Added in V3 |

**Gaps in `user.addresses`:**
- `label` column missing (e.g. "Home", "Work") → added in V4

---

### `booking` schema — Mostly Complete

| OpenAPI Schema | DB Table | Status |
|---|---|---|
| Booking | `booking.bookings` | ✓ Mapped (notes, preferred_provider_id, time_slot added in V3) |
| StatusTimelineItem | `booking.status_timeline` | ✓ Added in V3 |
| TimeSlot | Computed (not stored) | ✓ N/A |

**Gaps:**
- `Booking.statusTimeline` → `booking.status_timeline` table added in V3, seeded
- `Booking.paymentStatus` → computed from `billing.payments` table (not stored on booking)

---

### `visit` schema — Aligned in V3

| OpenAPI Schema | DB Table | Status |
|---|---|---|
| Visit | `visit.visits` | ✓ clinical_notes, procedures, photos, recommendations, patient_signature, report_url, gps_lat/lng, notes added in V3 |
| Vitals | `visit.vitals` | ✓ Mapped |
| Escalation | `visit.escalations` | ✓ gps_lat/lng, notified_users added in V3 |

**Visit entity — `@ElementCollection` tables:**
- `visit.visit_procedures` (procedures_performed)
- `visit.visit_photos` (photos)
- `visit.visit_recommendations` (recommendations)

---

### `cms` schema — Mostly Complete

| OpenAPI Schema | DB Table | Status |
|---|---|---|
| Service | `cms.services` | ✓ reviews_count, currency added in V3 |
| ServicePackage | `cms.service_packages` | ✓ service_id, duration, active added in V3 |

**Note:** OpenAPI `ServicePackage.serviceId` maps to `cms.service_packages.service_id`.
The DB also keeps `service_ids` (BIGINT[]) for multi-service packages.

---

### `billing` schema — Completed in V3

| OpenAPI Schema | DB Table | Status |
|---|---|---|
| Payment | `billing.payments` | ✓ Added in V3 |
| Invoice | `billing.invoices` | ✓ Mapped |

---

### `notification` schema — Mostly Complete

| OpenAPI Schema | DB Table | Status |
|---|---|---|
| Notification | `notification.notifications` | ✓ created_at added in V3 |
| PushToken | `notification.push_tokens` | ✓ V2 |

---

## Enums — Status

| Enum | Values (OpenAPI) | Values (DB/Entity) | Status |
|---|---|---|---|
| Role | PATIENT, PROVIDER, OPERATOR, ADMIN, SUPERADMIN | Same | ✓ |
| BookingStatus | REQUESTED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED | Same | ✓ |
| VisitStatus | SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED | IN_PROGRESS, COMPLETED, CANCELLED, ESCALATED | ⚠ Entity has ESCALATED (internal), SCHEDULED missing |
| PaymentStatus | PENDING, PAID, REFUNDED, FAILED | Same | ✓ |
| ProviderStatus | PENDING, VERIFIED, ACTIVE, REJECTED, SUSPENDED | Same | ✓ |
| UrgencyType | MEDICAL_EMERGENCY, SUSPECTED_ABUSE, MEDICATION_ERROR, PATIENT_DECLINING, OTHER | MEDICAL_EMERGENCY, SUSPECTED_ABUSE, MEDICATION_ERROR, PATIENT_DECLINING, EQUIPMENT_FAILURE, OTHER | ⚠ EQUIPMENT_FAILURE extra in entity |
| Platform | IOS, ANDROID | IOS, ANDROID | ✓ |
| ServiceCategory | NURSING, THERAPY, LABORATORY, SPECIALIST | Same | ✓ |
| DayOfWeek | MONDAY–SUNDAY | Same | ✓ |

---

## Known Limitations (Non-Blocking)

1. **Visit.rating / visit_ratings table** — OpenAPI `Visit.rating` (1-5 stars) and `review` are
   returned on completed visits but no `visit_ratings` table exists. Currently not stored.
   Rating is submitted via `POST /visits/{id}/rating` and returned in Visit response.

2. **Booking.paymentStatus** — Not stored as a column on `booking.bookings`. Inferred from
   `billing.payments.status` at read time. This is correct (payment is the source of truth).

3. **ServicePackage.serviceIds (BIGINT[])** — OpenAPI `ServicePackage.serviceId` (singular UUID)
   maps to `service_id`. The existing `service_ids[]` array coexists for multi-service packages.

4. **Escalation.status field** — Not in OpenAPI `Escalation` response but present in DB.
   Internal use only. Services should not return it to API clients.

---

## Migration Summary

| Migration | Contents |
|---|---|
| V1__init.sql | All core tables (auth, user, booking, visit, cms, billing, notification, audit) |
| V2__add_notification_push_tokens.sql | `notification.push_tokens` table |
| V3__align_openapi_schema.sql | Visit clinical fields, escalation GPS/notified, provider availability, payments, status timeline, service/cms additions |
| V4 (future) | Address label, Visit SCHEDULED status, visit_ratings table |

---

## Running Migrations

```bash
# Dev environment
mvn flyway:migrate -Dspring-boot.run.profiles=dev

# Check current version
mvn flyway:info -Dspring-boot.run.profiles=dev
```
