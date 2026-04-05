package com.gov.ac.persistence;

import com.gov.ac.domain.communication.CircularRecipient;
import com.gov.ac.domain.communication.CircularRecipientId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CircularRecipientRepository
    extends JpaRepository<CircularRecipient, CircularRecipientId> {

  Optional<CircularRecipient> findByIdCircularIdAndIdUserId(UUID circularId, String userId);
}
