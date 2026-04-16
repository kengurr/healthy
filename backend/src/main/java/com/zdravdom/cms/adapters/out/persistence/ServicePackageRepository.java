package com.zdravdom.cms.adapters.out.persistence;

import com.zdravdom.cms.domain.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for ServicePackage entities.
 */
@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    @Query(value = "SELECT * FROM cms.service_packages WHERE :serviceId = ANY(service_ids)", nativeQuery = true)
    List<ServicePackage> findByServiceId(@Param("serviceId") Long serviceId);

    List<ServicePackage> findByActiveTrue();
}
