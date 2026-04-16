# Patient App — React Native (Expo)

## Platform
- iOS + Android (React Native via Expo)
- Responsive web (React Native Web)

## Context
This is the patient-facing mobile app for Zdravdom — a home healthcare platform. Patients use this to browse services, book home visits, track provider arrival, complete intake forms, rate visits, and manage their health documents.

## Key Screens (from UI/UX spec)
- P-01: Splash / Onboarding
- P-02: Login / Register (phone + password, or SSO Apple/Google)
- P-03: OTP Verification (SMS)
- P-04: Patient Home Dashboard
- P-05: Home (service catalog)
- P-06: Service Catalog / Service Detail
- P-07: Package Comparison (S/M/L)
- P-08: Date & Time Selection
- P-09: Provider Selection
- P-10: Confirm & Pay (Stripe)
- P-11: Booking Confirmed
- P-12: Live Visit Tracking (GPS, ETA)
- P-13: Visit Summary & Rating
- P-14: Booking History
- P-15: Profile & Settings
- P-16: Provider Profile View
- P-17: Privacy & GDPR (data export, delete)
- P-18: Medical Document Upload (OCR)

## Critical Rules

### Offline Behavior
- Visit form data must be auto-saved locally every 30 seconds (SQLite or AsyncStorage)
- Offline queue for form submissions — sync on reconnect
- Critical actions (payments, new bookings) disabled when offline with clear messaging

### GPS Tracking
- Opt-in only — never track without explicit consent
- Background location on iOS requires justification for App Store approval
- Use foreground-service on Android for live tracking
- Share ETA feature: one-time link with expiry (not live continuous sharing)

### Navigation
- React Navigation (bottom tabs + stack)
- Handle safe area on iPhone notch / dynamic island
- KeyboardAvoidingView for form inputs

### API Integration
- All API calls go through `@zdravdom/api-client` from shared packages
- NEVER call backend APIs directly from this app
- Auth tokens stored securely (Keychain on iOS, Keystore on Android)

### State Management
- React Context for auth state
- React Query / TanStack Query for server state
- Local state for UI-only concerns

## Active Skills
- mobile-development: React Native, Expo, offline queue, GPS
- react: component patterns, hooks, context
- typescript: strict mode

## Tech Stack
- React Native (Expo)
- TypeScript (strict)
- React Navigation
- React Query / TanStack Query
- react-native-stripe (card input)
- react-native-signature-canvas (e-signature)
- AsyncStorage / SQLite (offline storage)

## See Also
- Root CLAUDE.md for project overview
- `/shared/CLAUDE.md` for shared TypeScript packages (api-client, types, hooks)
- `/shared/packages/types/` for all shared interfaces
- `/shared/packages/api-client/` for generated API client