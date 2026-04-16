# Shared TypeScript Packages

## Purpose
This folder contains npm packages that are consumed by ALL three frontend applications (Patient App, Provider App, Admin Portal). They are the single source of truth for shared types, API clients, hooks, and constants.

## Package Structure

```
shared/
├── packages/
│   ├── api-client/     → Auto-generated from OpenAPI spec. DO NOT EDIT MANUALLY.
│   │   ├── patientApi.ts
│   │   ├── providerApi.ts
│   │   ├── adminApi.ts
│   │   └── index.ts
│   ├── types/           → Shared TypeScript interfaces (Patient, Provider, Booking, Visit, etc.)
│   │   ├── index.ts
│   │   ├── patient.types.ts
│   │   ├── provider.types.ts
│   │   ├── booking.types.ts
│   │   └── visit.types.ts
│   ├── hooks/           → Shared React hooks (useAuth, useBooking, useNotifications, etc.)
│   │   ├── index.ts
│   │   ├── useAuth.ts
│   │   └── useApi.ts
│   └── constants/        → Shared constants (API_BASE_URL, ROUTES, THEME, etc.)
│       ├── index.ts
│       ├── routes.ts
│       └── theme.ts
└── package.json         → npm workspace root
```

## Critical Rules

### api-client — AUTO-GENERATED, DO NOT EDIT
- Generated from OpenAPI spec at `/backend/api-contract/openapi.yaml`
- Run `npm run generate:api` to regenerate after OpenAPI changes
- Manual edits to this package will be overwritten on next generation

### types — ONE SOURCE OF TRUTH
- All shared TypeScript interfaces live here
- Frontend apps import from `@zdravdom/types` — not from each other
- If a type changes, update here first, then regenerate api-client

### Breaking Changes
- Any breaking change to shared packages requires:
  1. Major version bump (e.g., 1.x.x → 2.0.0)
  2. Migration guide in `/docs/migrations/`
  3. Notification to all consuming app teams

### Publishing
- Run `npm run build && npm run test` before publishing
- Use semantic versioning
- Publish to npm or internal registry

## Active Skills
- typescript: strict mode, advanced patterns
- api-development: OpenAPI spec management, contract-first

## Tech Stack
- TypeScript (strict mode enabled)
- npm workspaces (monorepo structure)
- Vitest for testing
- ESLint + Prettier

## See Also
- Root CLAUDE.md for project overview
- `/backend/CLAUDE.md` for backend API contract
- `/patient-app/CLAUDE.md`, `/provider-app/CLAUDE.md`, `/admin-portal/CLAUDE.md` for app-specific rules