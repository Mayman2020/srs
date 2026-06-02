package com.gov.ac.feature.notification.inbox.repository;

import com.gov.ac.feature.notification.inbox.entity.InAppNotificationEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, UUID> {

  @EntityGraph(attributePaths = {"eventType", "recipient"})
  Page<InAppNotificationEntity> findByRecipient_IdAndDeletedAtIsNull(UUID recipientId, Pageable pageable);

  @EntityGraph(attributePaths = {"eventType", "recipient"})
  Optional<InAppNotificationEntity> findByIdAndRecipient_IdAndDeletedAtIsNull(UUID id, UUID recipientId);

  /** Returns all unread, non-deleted notifications for a recipient (used by mark-all-read). */
  List<InAppNotificationEntity> findByRecipient_IdAndDeletedAtIsNullAndReadAtIsNull(UUID recipientId);

  /** Bulk-marks all unread notifications for a recipient as read in a single UPDATE. */
  @Modifying
  @Query("UPDATE InAppNotificationEntity n SET n.readAt = :now WHERE n.recipient.id = :recipientId AND n.deletedAt IS NULL AND n.readAt IS NULL")
  int markAllReadForRecipient(@Param("recipientId") UUID recipientId, @Param("now") Instant now);
}
