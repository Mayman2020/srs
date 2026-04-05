package com.gov.ac.domain.communication;

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
@Table(name = "circular_recipient")
@Getter
@Setter
@NoArgsConstructor
public class CircularRecipient {

  @EmbeddedId private CircularRecipientId id = new CircularRecipientId();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("circularId")
  @JoinColumn(name = "circular_id")
  private Circular circular;

  @Column(name = "read_at")
  private Instant readAt;
}
