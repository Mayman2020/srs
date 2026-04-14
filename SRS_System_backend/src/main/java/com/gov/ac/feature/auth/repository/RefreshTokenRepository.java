package com.gov.ac.feature.auth.repository;

import com.gov.ac.feature.auth.entity.RefreshTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

  Optional<RefreshTokenEntity> findByJti(UUID jti);

  void deleteByUser_Id(UUID userId);
}
