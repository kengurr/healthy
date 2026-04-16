# Admin Portal — React (Vite)

## Platform
- Browser only (React web application)
- No mobile version

## Context
This is the operations/back-office web application for Zdravdom — used by admin staff, dispatchers, and superadmins to manage users, providers, bookings, content, analytics, and support workflows.

## Key Screens (from UI/UX spec)
- A-01: Admin Login
- A-02: Operations Dashboard (KPIs, DAU/MAU, booking stats)
- A-03: Provider Verification Queue
- A-04: Booking Management & Dispatch
- A-05: Service & Pricing CMS
- A-06: Analytics Dashboard
- A-07: Support Tools
- A-08: GDPR & Compliance (data export, delete requests, audit logs)
- A-09: Escalations Queue (red button alerts)

## Critical Rules

### Role-Based Access Control (RBAC)
5 roles with different navigation and capabilities:
| Role | Access |
|---|---|
| OPERATOR | Booking queue, provider management, support |
| ADMIN | Everything + user management, CMS, analytics |
| SUPERADMIN | Everything + system configuration, audit logs |

- Navigation items hidden based on role
- API endpoints enforce RBAC (backend also validates)
- Audit logging on all admin actions (immutable log)

### Admin-Specific UX
- This is a power user tool — not a consumer app
- Dense data tables with filters, sort, pagination
- Quick actions (approve/reject provider, cancel booking, etc.)
- Keyboard shortcuts for common actions
- Batch operations support

### Data Management
- GDPR workflows: data export requests, delete requests, consent records
- Audit trail viewer for compliance
- Manual booking intervention capability
- Provider and patient data lookup

### Tech Stack
- React 18+ (Vite)
- TypeScript (strict)
- React Router v6
- React Query / TanStack Query (server state)
- Component library: shadcn/ui or similar (Radix UI based)
- Chart library: Recharts or Tremor (analytics)

## Active Skills
- web-development: React patterns, component library, routing
- react: component patterns, hooks, context
- typescript: strict mode

## See Also
- Root CLAUDE.md for project overview
- `/shared/CLAUDE.md` for shared TypeScript packages
- `/backend/CLAUDE.md` for backend API contract