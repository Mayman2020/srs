package com.gov.ac.persistence;

import com.gov.ac.domain.auth.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByJti(UUID jti);

  void deleteByUser_Id(UUID userId);
}
