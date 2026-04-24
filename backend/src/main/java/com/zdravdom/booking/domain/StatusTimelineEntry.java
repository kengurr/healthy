package com.zdravdom.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Status timeline entry for booking audit trail.
 * Maps to booking.status_timeline table.
 */
@Entity
@Table(name = "status_timeline", schema = "booking")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusTimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Booking.BookingStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    public StatusTimelineEntry(Long bookingId, Booking.BookingStatus status, String note, Long createdBy) {
        this.bookingId = bookingId;
        this.status = status;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    /** Convenience constructor without createdBy (for admin operations). */
    public StatusTimelineEntry(Long bookingId, Booking.BookingStatus status, String note) {
        this(bookingId, status, note, null);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
