package com.zdravdom.user.application.mapper;

import com.zdravdom.user.application.dto.UpdateAvailabilityRequest;
import com.zdravdom.user.domain.Provider;

/**
 * Maps availability update fields onto an existing Provider entity.
 */
public final class ProviderMapper {

    private ProviderMapper() {}

    /**
     * Apply availability update request to provider.
     * Note: actual schedule persistence is handled directly in ProviderService
     * via ProviderScheduleRepository — this method is reserved for future
     * provider-level fields if needed.
     *
     * @param provider the provider entity
     * @param request  the availability update request
     */
    public static void updateAvailabilityFromRequest(Provider provider,
            UpdateAvailabilityRequest request) {
        // No-op: schedule data is persisted directly by ProviderService
    }
}
