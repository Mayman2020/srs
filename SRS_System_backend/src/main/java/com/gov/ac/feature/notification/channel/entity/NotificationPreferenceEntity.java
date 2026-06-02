package com.gov.ac.feature.notification.channel.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_preference", schema = "srs_system")
@Getter
@Setter
public class NotificationPreferenceEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "event_type_code", nullable = false, length = 64)
  private String eventTypeCode;

  @Column(name = "channel_code", nullable = false, length = 32)
  private String channelCode;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;
}
