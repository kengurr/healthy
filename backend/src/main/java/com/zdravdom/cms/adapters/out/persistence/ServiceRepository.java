package com.zdravdom.cms.adapters.out.persistence;

import com.zdravdom.cms.domain.Service;
import com.zdravdom.cms.domain.Service.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Service entities.
 */
@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByCategory(ServiceCategory category);

    List<Service> findByActiveTrue();

    List<Service> findByNameContainingIgnoreCase(String name);
}