package com.zdravdom.cms.adapters.out.persistence;

import com.zdravdom.cms.domain.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for ServicePackage entities.
 */
@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    List<ServicePackage> findByServiceId(Long serviceId);

    List<ServicePackage> findByActiveTrue();
}