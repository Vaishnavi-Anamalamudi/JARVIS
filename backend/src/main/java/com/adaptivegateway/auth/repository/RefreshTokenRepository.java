package com.adaptivegateway.auth.repository;

import com.adaptivegateway.auth.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndDeletedAtIsNull(String tokenHash);
}
