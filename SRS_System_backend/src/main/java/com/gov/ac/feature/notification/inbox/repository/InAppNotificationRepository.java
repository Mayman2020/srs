package com.gov.ac.feature.notification.inbox.repository;

import com.gov.ac.feature.notification.inbox.entity.InAppNotificationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, UUID> {

  @EntityGraph(attributePaths = {"eventType", "recipient"})
  Page<InAppNotificationEntity> findByRecipient_IdAndDeletedAtIsNull(UUID recipientId, Pageable pageable);

  @EntityGraph(attributePaths = {"eventType", "recipient"})
  Optional<InAppNotificationEntity> findByIdAndRecipient_IdAndDeletedAtIsNull(UUID id, UUID recipientId);
}
