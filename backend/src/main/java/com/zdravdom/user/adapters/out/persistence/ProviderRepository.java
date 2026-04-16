package com.zdravdom.user.adapters.out.persistence;

import com.zdravdom.user.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Provider entities.
 */
@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByEmail(String email);

    Optional<Provider> findByUserId(UUID userId);

    boolean existsByEmail(String email);

    java.util.List<Provider> findByStatus(Provider.ProviderStatus status);
}