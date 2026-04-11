package com.gov.ac.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_event", schema = "srs_system")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

  @Id private UUID id;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "actor_user_id", nullable = false)
  private String actorUserId;

  @Column(name = "action_code", nullable = false, length = 128)
  private String actionCode;

  @Column(name = "resource_type", length = 128)
  private String resourceType;

  @Column(name = "resource_id")
  private String resourceId;

  @Column(name = "detail_json", columnDefinition = "TEXT")
  private String detailJson;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }
}
