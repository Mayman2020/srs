package com.gov.ac.domain.notification;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.NotificationEventType;
import com.gov.ac.domain.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class InAppNotification extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_user_id", nullable = false)
  private AppUser recipient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notification_event_type_id", nullable = false)
  private NotificationEventType eventType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correspondence_id")
  private Correspondence correspondence;

  @Column(name = "title_ar")
  private String titleAr;

  @Column(name = "title_en")
  private String titleEn;

  @Column(name = "body_ar", columnDefinition = "text")
  private String bodyAr;

  @Column(name = "body_en", columnDefinition = "text")
  private String bodyEn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> data;

  @Column(name = "message_key")
  private String messageKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "message_params", columnDefinition = "jsonb")
  private Map<String, Object> messageParams;

  @Column(name = "read_at")
  private Instant readAt;
}
