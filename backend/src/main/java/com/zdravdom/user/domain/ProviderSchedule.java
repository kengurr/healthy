package com.zdravdom.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Provider weekly schedule entry or blocked date.
 * Maps to provider_schedule table.
 */
@Entity
@Table(name = "provider_schedule")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    @Column(name = "blocked_date")
    private LocalDate blockedDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    public static ProviderSchedule availableSlot(Long providerId, DayOfWeek day, LocalTime start, LocalTime end) {
        ProviderSchedule s = new ProviderSchedule();
        s.providerId = providerId;
        s.dayOfWeek = day;
        s.startTime = start;
        s.endTime = end;
        s.isBlocked = false;
        return s;
    }

    public static ProviderSchedule blocked(Long providerId, LocalDate date) {
        ProviderSchedule s = new ProviderSchedule();
        s.providerId = providerId;
        s.blockedDate = date;
        s.isBlocked = true;
        return s;
    }
}
