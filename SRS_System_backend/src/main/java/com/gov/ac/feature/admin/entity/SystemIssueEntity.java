package com.gov.ac.feature.admin.entity;

import com.gov.ac.feature.users.entity.AppUserEntity;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "system_issue", schema = "srs_system")
@Getter
@Setter
public class SystemIssueEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 16)
  private String source;

  @Column(nullable = false, length = 16)
  private String severity = "ERROR";

  @Column(nullable = false, columnDefinition = "text")
  private String message;

  @Column(columnDefinition = "text")
  private String detail;

  @Column(name = "page_url", length = 2000)
  private String pageUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private AppUserEntity user;

  @Column(name = "http_status")
  private Integer httpStatus;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolved_by")
  private UUID resolvedBy;

  @Column(name = "resolution_note", columnDefinition = "text")
  private String resolutionNote;
}
