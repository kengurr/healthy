package com.zdravdom.booking.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * TimeSlot embedded in Booking.
 * Maps to start_time and end_time columns in booking.bookings.
 */
@Embeddable
public class TimeSlot {

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Default constructor for JPA
    public TimeSlot() {}

    public TimeSlot(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }

    public boolean overlaps(TimeSlot other) {
        if (other == null) return false;
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }
}
