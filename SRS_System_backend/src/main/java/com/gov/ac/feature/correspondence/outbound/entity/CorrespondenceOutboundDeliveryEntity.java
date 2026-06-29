package com.gov.ac.feature.correspondence.outbound.entity;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "correspondence_outbound_delivery", schema = "srs_system")
@Getter
@Setter
public class CorrespondenceOutboundDeliveryEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @Column(name = "channel_code", nullable = false, length = 32)
  private String channelCode;

  @Column(name = "status_code", nullable = false, length = 32)
  private String statusCode = "PENDING";

  @Column(name = "recipient_label", length = 500)
  private String recipientLabel;

  @Column(name = "proof_reference", length = 256)
  private String proofReference;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;
}
