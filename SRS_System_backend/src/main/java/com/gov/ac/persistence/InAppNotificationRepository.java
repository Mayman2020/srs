package com.gov.ac.persistence;

import com.gov.ac.domain.notification.InAppNotification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

  @EntityGraph(attributePaths = {"eventType", "recipient"})
  Page<InAppNotification> findByRecipient_IdAndDeletedAtIsNull(UUID recipientId, Pageable pageable);

  @EntityGraph(attributePaths = {"eventType", "recipient"})
  Optional<InAppNotification> findByIdAndRecipient_IdAndDeletedAtIsNull(UUID id, UUID recipientId);
}
