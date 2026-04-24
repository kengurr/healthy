package com.zdravdom.booking.adapters.out.persistence;

import com.zdravdom.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Booking entities.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByPatientId(Long patientId);

    List<Booking> findByProviderId(Long providerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.providerId = :providerId")
    long countByProviderId(@Param("providerId") Long providerId);

    List<Booking> findByStatus(Booking.BookingStatus status);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    List<Booking> findByDate(LocalDate date);

    List<Booking> findByDateAndProviderId(LocalDate date, Long providerId);

    List<Booking> findByPatientIdAndStatus(Long patientId, Booking.BookingStatus status);

    List<Booking> findByStatusIn(List<Booking.BookingStatus> statuses);

    List<Booking> findByDateBetween(LocalDate startDate, LocalDate endDate);
}