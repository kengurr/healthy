# Admin Portal

Operations/back-office web application for Zdravdom — used by admin staff, dispatchers, and superadmins to manage users, providers, bookings, content, analytics, and support workflows.

## Tech Stack

- React 19 + Vite
- TypeScript (strict)
- TanStack Query (server state)
- React Router v6

## Getting Started

```bash
npm install
npm run dev
```

Opens at `http://localhost:5173`

## Dev Login

For local development with the backend running on `http://localhost:8080`:

- **Email**: `admin@zdravdom.com`
- **Password**: `adminpassword123`

This admin account is seeded by `backend/src/main/resources/db/migration/V0__init_dev.sql`.

## Backend Connection

The portal expects the backend API at `http://localhost:8080` with JWT authentication. Start the backend with:

```bash
cd ../backend && mvn spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev
```

The dev profile uses an insecure JWT secret (`dev-only-secret-key-not-for-production-use-minimum-32-chars`) and has CORS configured to allow `localhost:5173`.

## Key Screens

- **Dashboard** (`/`) — KPIs, booking stats, DAU/MAU
- **Provider Verification** (`/providers`) — approve/reject pending providers
- **Bookings** (`/bookings`) — manage and dispatch bookings
- **Services CMS** (`/services`) — manage services and packages
- **Escalations** (`/escalations`) — handle red-button alerts
- **Users** (`/users`) — patient account management

## Roles

| Role | Access |
|------|--------|
| OPERATOR | Booking queue, provider management, support |
| ADMIN | Everything + user management, CMS, analytics |
| SUPERADMIN | Everything + system configuration, audit logs |

The seeded dev admin has the SUPERADMIN role.
