package com.gov.ac.feature.communication.repository;

import com.gov.ac.feature.communication.entity.CircularRecipientEntity;
import com.gov.ac.feature.communication.entity.CircularRecipientId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CircularRecipientRepository
    extends JpaRepository<CircularRecipientEntity, CircularRecipientId> {

  Optional<CircularRecipientEntity> findByIdCircularIdAndIdUserId(UUID circularId, String userId);
}
