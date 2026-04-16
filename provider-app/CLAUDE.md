# Provider App — React Native (Expo)

## Platform
- iOS + Android (React Native via Expo)
- Web dashboard (React Native Web)

## Context
This is the provider-facing mobile app for Zdravdom — a home healthcare platform. Providers (nurses, physiotherapists, doctors, etc.) use this to manage their availability, receive booking requests, navigate to visits, complete structured clinical forms, generate PDF reports, and track earnings.

## Key Screens (from UI/UX spec)
- PR-01: Splash / Onboarding
- PR-02: Login / Register
- PR-03: Provider Dashboard (home)
- PR-04: Availability Calendar
- PR-05: Booking Inbox (accept/reject requests)
- PR-06: Booking Detail View
- PR-07: Navigation (Google Maps / Waze deep links)
- PR-08: Visit Form (structured clinical data capture)
- PR-09: Earnings Dashboard
- PR-10: Profile & Documents
- PR-11: Red Button Escalation (urgent clinical finding)

## Critical Rules

### Offline Capability (HIGH PRIORITY)
- Provider app MUST work offline with cached schedule
- Visit forms auto-saved locally every 30 seconds
- Actions queued offline must sync reliably on reconnect
- Conflict resolution: server wins for schedule, client wins for form drafts
- Critical actions (payments, new acceptances) disabled when offline

### Visit Workflow
- Structured visit forms by service type (nursing, physiotherapy, wound care, etc.)
- Vital signs capture: BP, heart rate, temperature, SpO2, respiratory rate
- Photo upload for wound assessment (EXIF rotation handled, compressed)
- Home exercise planning as part of visit completion
- PDF report auto-generated after visit completion (backend async job)
- Patient e-signature capture on completion

### Red Button Escalation
- Safety-critical feature — failure modes must be thoroughly designed
- Triggers: Medical Emergency, Suspected Abuse, Medication Error, Patient Declining Rapidly
- Auto-captured data: patient name, visit ID, provider ID, GPS location, timestamp
- Notifications sent to: on-call doctor + operations team simultaneously
- Fallback to SMS if push notification fails

### Navigation
- Google Maps / Waze deep links for route guidance
- Route optimization for multiple visits in a day (Google Distance Matrix API)
- Offline map tiles consideration for areas with poor connectivity

### API Integration
- All API calls via `@zdravdom/api-client` from shared packages
- Auth tokens stored securely (Keychain/Keystore)
- Calendar sync with Google/Apple/CalDAV — read-only in MVP, bidirectional in Phase 2+

### Provider Verification Workflow
- Document upload: license, diploma, insurance policy, identity document
- OCR extraction with manual review (backend workflow)
- Status transitions: pending → verified → active → rejected → suspended
- Auto-accept option configurable by provider

## Active Skills
- mobile-development: React Native, Expo, offline queue, GPS
- react: component patterns, hooks, context
- typescript: strict mode

## Tech Stack
- React Native (Expo)
- TypeScript (strict)
- React Navigation
- React Query / TanStack Query
- react-native-signature-canvas (e-signature)
- AsyncStorage / SQLite (offline storage)
- react-native-maps (optional for embedded map)

## See Also
- Root CLAUDE.md for project overview
- `/shared/CLAUDE.md` for shared TypeScript packages
- `/backend/CLAUDE.md` for backend services and API contract