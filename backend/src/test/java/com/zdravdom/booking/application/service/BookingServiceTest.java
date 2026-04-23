package com.zdravdom.booking.application.service;

import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.adapters.out.persistence.StatusTimelineRepository;
import com.zdravdom.booking.application.dto.BookingListResponse;
import com.zdravdom.booking.application.dto.BookingResponse;
import com.zdravdom.booking.application.dto.CreateBookingRequest;
import com.zdravdom.booking.application.dto.TimeSlotResponse;
import com.zdravdom.booking.application.service.BookingService;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.booking.domain.Booking.BookingStatus;
import com.zdravdom.booking.domain.Booking.PaymentStatus;
import com.zdravdom.booking.domain.StatusTimelineEntry;
import com.zdravdom.booking.domain.TimeSlot;
import com.zdravdom.cms.adapters.out.persistence.ServiceRepository;
import com.zdravdom.cms.domain.Service;
import com.zdravdom.global.exception.GlobalExceptionHandler.ConflictException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.matching.application.service.SlotLockService;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private StatusTimelineRepository timelineRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private SlotLockService slotLockService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
            bookingRepository, timelineRepository, serviceRepository, patientRepository, slotLockService);
    }

    // ─── createBooking ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking()")
    class CreateBooking {

        @Test
        @DisplayName("saves booking with correct initial state")
        void savesBooking_WithCorrectInitialState() {
            CreateBookingRequest request = new CreateBookingRequest(
                1L, null, 100L, LocalDate.now().plusDays(1), "09:00", null, "First visit"
            );

            when(patientRepository.existsById(1L)).thenReturn(true);
            when(serviceRepository.existsById(1L)).thenReturn(true);
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(makeService(1L)));
            when(bookingRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking b = invocation.getArgument(0);
                b.setId(10L);
                return b;
            });
            when(timelineRepository.save(any(StatusTimelineEntry.class))).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.createBooking(1L, request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.status()).isEqualTo(BookingStatus.REQUESTED);
            assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(response.patientId()).isEqualTo(1L);
            assertThat(response.serviceId()).isEqualTo(1L);
            assertThat(response.addressId()).isEqualTo(100L);

            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(bookingCaptor.capture());
            Booking saved = bookingCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(BookingStatus.REQUESTED);
            assertThat(saved.getPaymentAmount()).isEqualTo(BigDecimal.valueOf(45.00));

            verify(timelineRepository).save(any(StatusTimelineEntry.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when patient not found")
        void throwsNotFound_WhenPatientMissing() {
            CreateBookingRequest request = new CreateBookingRequest(
                999L, null, 100L, LocalDate.now().plusDays(1), "09:00", null, null
            );
            when(patientRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> bookingService.createBooking(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when service not found")
        void throwsNotFound_WhenServiceMissing() {
            CreateBookingRequest request = new CreateBookingRequest(
                1L, null, 100L, LocalDate.now().plusDays(1), "09:00", null, null
            );
            when(patientRepository.existsById(1L)).thenReturn(true);
            when(serviceRepository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service");
        }

        @Test
        @DisplayName("throws ConflictException for duplicate idempotency key")
        void throwsConflict_WhenDuplicateIdempotencyKey() {
            CreateBookingRequest request = new CreateBookingRequest(
                1L, null, 100L, LocalDate.now().plusDays(1), "09:00", null, null
            );
            when(patientRepository.existsById(1L)).thenReturn(true);
            when(serviceRepository.existsById(1L)).thenReturn(true);
            when(bookingRepository.findByIdempotencyKey(any())).thenReturn(Optional.of(new Booking()));

            assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
        }
    }

    // ─── cancelBooking ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking()")
    class CancelBooking {

        @Test
        @DisplayName("cancels a CONFIRMED booking")
        void cancelsConfirmedBooking() {
            Booking booking = makeBooking(10L, 1L, BookingStatus.CONFIRMED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
            when(timelineRepository.save(any(StatusTimelineEntry.class))).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.cancelBooking(10L, 1L, "Changed plans");

            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(response.cancellationReason()).isEqualTo("Changed plans");

            verify(timelineRepository).save(argThat(entry ->
                entry.getStatus() == BookingStatus.CANCELLED));
        }

        @Test
        @DisplayName("cancels a REQUESTED booking")
        void cancelsRequestedBooking() {
            Booking booking = makeBooking(10L, 1L, BookingStatus.REQUESTED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
            when(timelineRepository.save(any(StatusTimelineEntry.class))).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.cancelBooking(10L, 1L, null);

            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("throws ValidationException for IN_PROGRESS booking")
        void throwsValidation_ForInProgressBooking() {
            Booking booking = makeBooking(10L, 1L, BookingStatus.IN_PROGRESS);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(10L, 1L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be cancelled");
        }

        @Test
        @DisplayName("throws ValidationException when user does not own booking")
        void throwsValidation_WhenUserNotOwner() {
            Booking booking = makeBooking(10L, 1L, BookingStatus.CONFIRMED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(10L, 999L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Not authorized");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when booking not found")
        void throwsNotFound_WhenBookingMissing() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancelBooking(999L, 1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── acceptBooking ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptBooking()")
    class AcceptBooking {

        @Test
        @DisplayName("accepts a REQUESTED booking and sets CONFIRMED status")
        void acceptsRequestedBooking() {
            Booking booking = makeBooking(10L, 2L, BookingStatus.REQUESTED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
            when(timelineRepository.save(any(StatusTimelineEntry.class))).thenAnswer(i -> i.getArgument(0));
            when(slotLockService.tryLock(anyLong(), any(), any(), anyString())).thenReturn(true);

            BookingResponse response = bookingService.acceptBooking(10L, 5L);

            assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(response.providerId()).isEqualTo(5L);

            verify(timelineRepository).save(argThat(entry ->
                entry.getStatus() == BookingStatus.CONFIRMED));
        }

        @Test
        @DisplayName("throws ValidationException for non-REQUESTED booking")
        void throwsValidation_ForNonRequested() {
            Booking booking = makeBooking(10L, 2L, BookingStatus.CONFIRMED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.acceptBooking(10L, 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot accept");
        }
    }

    // ─── rejectBooking ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectBooking()")
    class RejectBooking {

        @Test
        @DisplayName("rejects a REQUESTED booking and sets CANCELLED status (not CONFIRMED)")
        void rejectsRequestedBooking_SetsCancelledNotConfirmed() {
            Booking booking = makeBooking(10L, 2L, BookingStatus.REQUESTED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
            when(timelineRepository.save(any(StatusTimelineEntry.class))).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.rejectBooking(10L, 5L, "Schedule conflict");

            // This is the critical bug that was fixed
            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(response.cancellationReason()).isEqualTo("Schedule conflict");

            verify(timelineRepository).save(argThat(entry ->
                entry.getStatus() == BookingStatus.CANCELLED));
        }

        @Test
        @DisplayName("throws ValidationException for non-REQUESTED booking")
        void throwsValidation_ForNonRequested() {
            Booking booking = makeBooking(10L, 2L, BookingStatus.CONFIRMED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.rejectBooking(10L, 5L, "reason"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot reject");
        }
    }

    // ─── getBookingById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBookingById()")
    class GetBookingById {

        @Test
        @DisplayName("returns booking with timeline")
        void returnsBookingWithTimeline() {
            Booking booking = makeBooking(10L, 1L, BookingStatus.CONFIRMED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
            when(timelineRepository.findByBookingIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(
                new StatusTimelineEntry(10L, BookingStatus.REQUESTED, "Created", 1L),
                new StatusTimelineEntry(10L, BookingStatus.CONFIRMED, "Accepted", 2L)
            ));

            BookingResponse response = bookingService.getBookingById(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(response.statusTimeline()).hasSize(2);
            assertThat(response.statusTimeline().get(0).status()).isEqualTo(BookingStatus.REQUESTED);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFound_WhenMissing() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBookingById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Booking makeBooking(Long id, Long patientId, BookingStatus status) {
        Booking b = new Booking();
        b.setId(id);
        b.setPatientId(patientId);
        b.setProviderId(2L);
        b.setServiceId(1L);
        b.setAddressId(100L);
        b.setDate(LocalDate.now().plusDays(1));
        b.setTimeSlot(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0)));
        b.setStatus(status);
        b.setPaymentAmount(BigDecimal.valueOf(45.00));
        b.setPaymentStatus(PaymentStatus.PENDING);
        b.setCreatedAt(LocalDateTime.now().minusHours(1));
        b.setUpdatedAt(LocalDateTime.now());
        b.setIdempotencyKey(UUID.randomUUID().toString());
        return b;
    }

    private Service makeService(Long id) {
        Service s = Service.create();
        s.setId(id);
        s.setPrice(BigDecimal.valueOf(45.00));
        return s;
    }
}
