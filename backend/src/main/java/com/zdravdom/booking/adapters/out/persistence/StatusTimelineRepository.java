package com.zdravdom.booking.adapters.out.persistence;

import com.zdravdom.booking.domain.StatusTimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for booking status timeline entries.
 */
@Repository
public interface StatusTimelineRepository extends JpaRepository<StatusTimelineEntry, Long> {

    List<StatusTimelineEntry> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}
