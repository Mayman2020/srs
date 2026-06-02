package com.gov.ac.feature.notification.channel.repository;

import com.gov.ac.feature.notification.channel.entity.NotificationChannelTargetEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationChannelTargetRepository
    extends JpaRepository<NotificationChannelTargetEntity, UUID> {

  List<NotificationChannelTargetEntity> findByChannelCodeAndEnabledTrueAndDeletedAtIsNull(
      String channelCode);

  Optional<NotificationChannelTargetEntity> findByChannelCodeAndTargetCodeAndDeletedAtIsNull(
      String channelCode, String targetCode);
}
