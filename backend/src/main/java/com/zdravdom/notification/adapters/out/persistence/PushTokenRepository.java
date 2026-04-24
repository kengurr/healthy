package com.zdravdom.notification.adapters.out.persistence;

import com.zdravdom.notification.domain.Platform;
import com.zdravdom.notification.domain.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for PushToken entities.
 */
@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByUserIdAndPlatform(Long userId, PushToken.Platform platform);

    List<PushToken> findByUserIdAndActiveTrue(Long userId);

    Optional<PushToken> findByToken(String token);

    void deleteByUserId(Long userId);
}