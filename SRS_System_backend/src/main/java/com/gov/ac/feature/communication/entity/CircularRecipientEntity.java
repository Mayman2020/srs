package com.gov.ac.feature.communication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "circular_recipient", schema = "srs_system")
@Getter
@Setter
@NoArgsConstructor
public class CircularRecipientEntity {

  @EmbeddedId private CircularRecipientId id = new CircularRecipientId();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("circularId")
  @JoinColumn(name = "circular_id")
  private CircularEntity circular;

  @Column(name = "read_at")
  private Instant readAt;
}
