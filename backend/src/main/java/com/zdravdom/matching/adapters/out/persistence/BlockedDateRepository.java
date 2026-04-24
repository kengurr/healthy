package com.zdravdom.matching.adapters.out.persistence;

import com.zdravdom.matching.domain.BlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * JPA repository for BlockedDate entities.
 */
@Repository
public interface BlockedDateRepository extends JpaRepository<BlockedDate, Long> {

    List<BlockedDate> findByProviderIdAndBlockedDate(Long providerId, LocalDate blockedDate);

    boolean existsByProviderIdAndBlockedDate(Long providerId, LocalDate blockedDate);
}
