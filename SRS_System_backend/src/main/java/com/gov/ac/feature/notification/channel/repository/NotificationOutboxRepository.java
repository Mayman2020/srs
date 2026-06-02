package com.gov.ac.feature.notification.channel.repository;

import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository
    extends JpaRepository<NotificationOutboxEntity, UUID>, NotificationOutboxRepositoryCustom {

  Page<NotificationOutboxEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<NotificationOutboxEntity> findAllByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
