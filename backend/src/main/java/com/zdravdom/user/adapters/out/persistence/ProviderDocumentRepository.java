package com.zdravdom.user.adapters.out.persistence;

import com.zdravdom.user.domain.ProviderDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for ProviderDocument entities.
 */
@Repository
public interface ProviderDocumentRepository extends JpaRepository<ProviderDocument, Long> {

    List<ProviderDocument> findByProviderId(Long providerId);

    List<ProviderDocument> findByProviderIdAndVerifiedFalse(Long providerId);
}
