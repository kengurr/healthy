package com.zdravdom.visit.application.dto;

import com.zdravdom.visit.domain.Escalation;

/**
 * Request to update an escalation status.
 */
public record UpdateEscalationStatusRequest(
    Escalation.EscalationStatus status,
    String resolution,
    String actionTaken
) {}