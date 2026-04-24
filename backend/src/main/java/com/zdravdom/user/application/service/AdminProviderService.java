package com.zdravdom.user.application.service;

import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.application.dto.ProviderVerificationItem;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin service for provider management — verification queue, approve/reject.
 */
@Service
public class AdminProviderService {

    private static final Logger log = LoggerFactory.getLogger(AdminProviderService.class);

    private final ProviderRepository providerRepository;

    public AdminProviderService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderVerificationItem> getVerificationQueue() {
        return providerRepository.findByStatus(ProviderStatus.PENDING_VERIFICATION).stream()
            .map(this::toVerificationItem)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Provider> getAllProviders(ProviderStatus status) {
        if (status != null) {
            return providerRepository.findByStatus(status);
        }
        return providerRepository.findAll();
    }

    @Transactional
    public void approveProvider(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));
        provider.setStatus(ProviderStatus.ACTIVE);
        provider.setVerified(true);
        providerRepository.save(provider);
        log.info("Provider {} approved and activated", providerId);
    }

    @Transactional
    public void rejectProvider(Long providerId, String reason) {
        Provider provider = providerRepository.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));
        provider.setStatus(ProviderStatus.SUSPENDED);
        providerRepository.save(provider);
        log.info("Provider {} rejected: {}", providerId, reason);
    }

    private ProviderVerificationItem toVerificationItem(Provider provider) {
        return new ProviderVerificationItem(
            provider.getId(),
            provider.getFirstName(),
            provider.getLastName(),
            provider.getEmail(),
            provider.getProfession(),
            provider.getSpecialty(),
            provider.getStatus(),
            List.of(), // document URLs — populated when ProviderDocument entity is fully integrated
            provider.getCreatedAt()
        );
    }
}