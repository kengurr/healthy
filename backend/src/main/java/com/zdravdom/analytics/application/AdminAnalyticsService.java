package com.zdravdom.analytics.application;

import com.zdravdom.analytics.application.dto.AdminDashboardResponse;
import com.zdravdom.billing.adapters.out.persistence.PaymentRepository;
import com.zdravdom.billing.domain.Payment;
import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.visit.adapters.out.persistence.EscalationRepository;
import com.zdravdom.visit.domain.Escalation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Admin analytics service — aggregates KPIs across all modules.
 */
@Service
public class AdminAnalyticsService {

    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final EscalationRepository escalationRepository;

    public AdminAnalyticsService(
            PatientRepository patientRepository,
            ProviderRepository providerRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            EscalationRepository escalationRepository) {
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.escalationRepository = escalationRepository;
    }

    public AdminDashboardResponse getDashboardStats() {
        long totalPatients = patientRepository.count();
        long totalProviders = providerRepository.count();
        long activeProviders = providerRepository.findByStatus(Provider.ProviderStatus.ACTIVE).size();
        long pendingVerifications = providerRepository.findByStatus(Provider.ProviderStatus.PENDING_VERIFICATION).size();

        long totalBookings = bookingRepository.count();
        var activeBookingStatuses = java.util.List.of(
            Booking.BookingStatus.CONFIRMED,
            Booking.BookingStatus.IN_PROGRESS
        );
        long activeBookings = bookingRepository.findByStatusIn(activeBookingStatuses).size();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        long bookingsToday = bookingRepository.findByDate(today).size();
        long bookingsThisWeek = bookingRepository.findByDateBetween(weekStart, today).size();

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime weekEnd = today.atTime(LocalTime.MAX);

        BigDecimal revenueToday = paymentRepository.sumAmountByStatusBetween(
            Payment.PaymentStatus.PAID, dayStart, dayEnd
        );

        BigDecimal revenueThisWeek = paymentRepository.sumAmountByStatusBetween(
            Payment.PaymentStatus.PAID, weekStart.atStartOfDay(), weekEnd
        );

        LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal revenueThisMonth = paymentRepository.sumAmountByStatusBetween(
            Payment.PaymentStatus.PAID, monthStart.atStartOfDay(), dayEnd
        );

        long openEscalations = escalationRepository.findByStatus(Escalation.EscalationStatus.OPEN).size();

        return new AdminDashboardResponse(
            totalPatients,
            totalProviders,
            activeProviders,
            totalBookings,
            activeBookings,
            bookingsToday,
            bookingsThisWeek,
            revenueToday,
            revenueThisWeek,
            revenueThisMonth,
            pendingVerifications,
            openEscalations,
            LocalDateTime.now()
        );
    }
}