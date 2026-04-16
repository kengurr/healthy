package com.zdravdom.auth.adapters.out.persistence;

import com.zdravdom.auth.domain.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for AuthToken entities.
 */
@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByRefreshTokenAndRevokedFalse(String refreshToken);

    Optional<AuthToken> findByUserIdAndRevokedFalse(Long userId);

    boolean existsByRefreshTokenAndRevokedFalse(String refreshToken);
}