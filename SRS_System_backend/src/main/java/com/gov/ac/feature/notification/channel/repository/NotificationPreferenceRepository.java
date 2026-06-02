package com.gov.ac.feature.notification.channel.repository;

import com.gov.ac.feature.notification.channel.entity.NotificationPreferenceEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreferenceEntity, UUID> {

  Optional<NotificationPreferenceEntity>
      findByUserIdAndEventTypeCodeAndChannelCodeAndDeletedAtIsNull(
          UUID userId, String eventTypeCode, String channelCode);

  List<NotificationPreferenceEntity> findAllByUserIdAndDeletedAtIsNull(UUID userId);
}
