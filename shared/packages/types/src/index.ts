// Zdravdom shared types — generated from OpenAPI spec
// Auto-generated from backend/api-contract/openapi.yaml

// ─── Enums ───────────────────────────────────────────────────────────────────

export type Role = 'PATIENT' | 'PROVIDER' | 'OPERATOR' | 'ADMIN' | 'SUPERADMIN';
export type ProviderStatus = 'PENDING_VERIFICATION' | 'VERIFIED' | 'ACTIVE' | 'REJECTED' | 'SUSPENDED' | 'INACTIVE';
export type BookingStatus = 'REQUESTED' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'REFUNDED' | 'FAILED';
export type VisitStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type UrgencyType = 'MEDICAL_EMERGENCY' | 'SUSPECTED_ABUSE' | 'MEDICATION_ERROR' | 'PATIENT_DECLINING' | 'OTHER';
export type Platform = 'IOS' | 'ANDROID';
export type ServiceCategory = 'NURSING' | 'THERAPY' | 'LABORATORY' | 'SPECIALIST';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

// ─── Shared ───────────────────────────────────────────────────────────────────

export interface Pagination {
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface ZdravdomError {
  code: string;
  message: string;
  field?: string;
  timestamp: string;
}

export interface Address {
  id: string;
  label: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  gpsLat?: number;
  gpsLng?: number;
  isDefault: boolean;
}

export interface EmergencyContact {
  name: string;
  phone: string;
  relation: string;
}

export interface InsuranceDetails {
  provider: string;
  policyNumber: string;
}

export interface GPSLocation {
  lat: number;
  lng: number;
}

export interface Vitals {
  bloodPressure?: string;
  heartRate?: number;
  temperature?: number;
  o2Saturation?: number;
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface RegisterRequest {
  email: string;
  phone: string;
  password: string;
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface ProviderRegisterRequest {
  email: string;
  phone: string;
  password: string;
  role: Role;
  profession: string;
  firstName: string;
  lastName: string;
}

// ─── Patient ──────────────────────────────────────────────────────────────────

export interface Patient {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dateOfBirth?: string;
  gender?: string;
  addresses: Address[];
  insuranceDetails?: InsuranceDetails;
  allergies: string[];
  chronicConditions: string[];
  emergencyContact?: EmergencyContact;
}

export interface UpdatePatientRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: string;
}

export interface AddAddressRequest {
  label: string;
  street: string;
  city: string;
  postalCode: string;
  country?: string;
  gpsLat?: number;
  gpsLng?: number;
  isDefault?: boolean;
}

export interface DocumentUploadRequest {
  type: string;
  description: string;
}

export interface GDPRExportResponse {
  jobId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  downloadUrl?: string;
}

// ─── Service / Catalog ────────────────────────────────────────────────────────

export interface Service {
  id: string;
  name: string;
  category: ServiceCategory;
  description?: string;
  duration: number;
  price: number;
  currency: string;
  rating?: number;
  reviewsCount?: number;
  imageUrl?: string;
  includedItems: string[];
}

export interface ServicePackage {
  id: string;
  serviceId: string;
  name: string;
  description?: string;
  price: number;
  duration: number;
}

export interface ProviderSummary {
  id: string;
  firstName: string;
  lastName: string;
  role: Role;
  profession: string;
  specialty?: string;
  rating?: number;
  reviewsCount?: number;
  distance?: number;
  languages: string[];
  experience?: number;
  bio?: string;
  photoUrl?: string;
  status: ProviderStatus;
}

// ─── Availability ────────────────────────────────────────────────────────────

export interface WeeklyScheduleItem {
  day: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface ProviderAvailability {
  weeklySchedule: WeeklyScheduleItem[];
  blockedDates: string[];
}

export interface UpdateAvailabilityRequest {
  weeklySchedule: WeeklyScheduleItem[];
  blockedDates: string[];
}

export interface TimeSlot {
  time: string;
  endTime: string;
  available: boolean;
}

export interface SlotQuery {
  serviceId: string;
  date: string;
  addressId: string;
}

// ─── Booking ──────────────────────────────────────────────────────────────────

export interface CreateBookingRequest {
  serviceId: string;
  packageId?: string;
  addressId: string;
  date: string;
  timeSlot: string;
  providerId?: string;
  notes?: string;
}

export interface StatusTimelineItem {
  status: BookingStatus;
  timestamp: string;
  note?: string;
}

export interface Booking {
  id: string;
  patientId: string;
  providerId: string;
  serviceId: string;
  packageId?: string;
  addressId: string;
  date: string;
  timeSlot: string;
  status: BookingStatus;
  paymentAmount?: number;
  paymentStatus: PaymentStatus;
  createdAt: string;
  statusTimeline: StatusTimelineItem[];
}

export interface BookingInbox {
  id: string;
  patientId: string;
  patientFirstName: string;
  patientLastName: string;
  serviceName: string;
  address: Address;
  date: string;
  timeSlot: string;
  status: BookingStatus;
}

export interface CancelBookingRequest {
  reason?: string;
}

export interface RejectBookingRequest {
  reason?: string;
}

export interface BookingListResponse {
  content: Booking[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

// ─── Visit ─────────────────────────────────────────────────────────────────────

export interface CompleteVisitRequest {
  vitals: Vitals;
  clinicalNotes: string;
  proceduresPerformed?: string[];
  photos?: string[];
  recommendations?: string;
  patientSignature: string;
}

export interface Visit {
  id: string;
  bookingId: string;
  providerId: string;
  patientId: string;
  vitals?: Vitals;
  clinicalNotes?: string;
  proceduresPerformed: string[];
  photos: string[];
  recommendations?: string;
  patientSignature?: string;
  status: VisitStatus;
  reportUrl?: string;
}

export interface EscalationRequest {
  urgencyType: UrgencyType;
  notes: string;
  gpsLocation?: GPSLocation;
}

export interface Escalation {
  id: string;
  visitId: string;
  urgencyType: UrgencyType;
  notes: string;
  gpsLocation?: GPSLocation;
  timestamp: string;
  notifiedUsers: string[];
}

// ─── Payments ─────────────────────────────────────────────────────────────────

export interface CreatePaymentIntentRequest {
  bookingId: string;
  allowedNetworks?: ('visa' | 'mastercard' | 'amex' | 'unionpay')[];
}

export interface PaymentIntentResponse {
  clientSecret: string;
  paymentIntentId: string;
}

export interface Payment {
  id: string;
  bookingId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  stripePaymentIntentId?: string;
  createdAt: string;
}

export interface RefundRequest {
  bookingId: string;
  amount: number;
  reason?: string;
}

// ─── Notifications ─────────────────────────────────────────────────────────────

export interface RegisterPushTokenRequest {
  token: string;
  platform: Platform;
}

export interface ZdravdomNotification {
  id: string;
  title: string;
  body: string;
  data?: Record<string, unknown>;
  read: boolean;
  createdAt: string;
}

// ─── Ratings ──────────────────────────────────────────────────────────────────

export interface CreateRatingRequest {
  rating: number;
  review?: string;
}

// ─── Pagination helpers ───────────────────────────────────────────────────────

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
}