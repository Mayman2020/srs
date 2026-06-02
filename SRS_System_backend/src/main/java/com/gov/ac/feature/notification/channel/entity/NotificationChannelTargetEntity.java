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
@Table(name = "notification_channel_target", schema = "srs_system")
@Getter
@Setter
public class NotificationChannelTargetEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "channel_code", nullable = false, length = 32)
  private String channelCode;

  @Column(name = "target_code", nullable = false, length = 64)
  private String targetCode;

  @Column(name = "target_url", length = 1024)
  private String targetUrl;

  @Column(name = "signing_secret_ref", length = 128)
  private String signingSecretRef;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "description", columnDefinition = "text")
  private String description;
}
