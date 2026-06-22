package com.gov.ac.feature.auth.repository;

import com.gov.ac.feature.auth.entity.PasswordResetTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

  Optional<PasswordResetTokenEntity> findFirstByTokenHashAndConsumedFalseOrderByCreatedAtDesc(
      String tokenHash);
}
