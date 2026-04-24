package com.zdravdom.user.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Patient response DTO for admin view.
 */
public record AdminPatientResponse(
    Long id,
    String email,
    String phone,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String gender,
    boolean verified,
    boolean active,
    LocalDateTime createdAt
) {}
