package com.zdravdom.visit.application.service;

import com.zdravdom.visit.adapters.out.persistence.EscalationRepository;
import com.zdravdom.visit.application.dto.AdminEscalationResponse;
import com.zdravdom.visit.application.dto.UpdateEscalationStatusRequest;
import com.zdravdom.visit.domain.Escalation;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin service for escalation queue management.
 */
@Service
public class AdminEscalationService {

    private static final Logger log = LoggerFactory.getLogger(AdminEscalationService.class);

    private final EscalationRepository escalationRepository;

    public AdminEscalationService(EscalationRepository escalationRepository) {
        this.escalationRepository = escalationRepository;
    }

    @Transactional(readOnly = true)
    public List<Escalation> getAllEscalations(Escalation.EscalationStatus status) {
        if (status != null) {
            return escalationRepository.findByStatus(status);
        }
        return escalationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AdminEscalationResponse getEscalationById(Long id) {
        Escalation escalation = escalationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Escalation", id));
        return toResponse(escalation);
    }

    @Transactional
    public AdminEscalationResponse updateStatus(Long id, UpdateEscalationStatusRequest request) {
        Escalation escalation = escalationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Escalation", id));
        if (request.status() != null) escalation.setStatus(request.status());
        if (request.resolution() != null) escalation.setResolution(request.resolution());
        if (request.actionTaken() != null) escalation.setActionTaken(request.actionTaken());
        if (request.status() == Escalation.EscalationStatus.RESOLVED) {
            escalation.setResolvedAt(LocalDateTime.now());
        }
        escalationRepository.save(escalation);
        log.info("Escalation {} status updated to {}", id, request.status());
        return toResponse(escalation);
    }

    private AdminEscalationResponse toResponse(Escalation e) {
        return new AdminEscalationResponse(
            e.getId(),
            e.getVisitId(),
            e.getProviderId(),
            null, // providerName — resolve via ProviderRepository if needed
            e.getPatientId(),
            null, // patientName — resolve via PatientRepository if needed
            e.getUrgencyType(),
            e.getStatus(),
            e.getDescription(),
            e.getActionTaken(),
            e.getResolution(),
            e.getCreatedAt(),
            e.getResolvedAt(),
            e.getGpsLat(),
            e.getGpsLng(),
            e.getNotifiedUsers() != null ? new java.util.ArrayList<>(e.getNotifiedUsers()) : List.of()
        );
    }
}