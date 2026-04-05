package com.gov.ac.domain.communication;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CircularRecipientId implements Serializable {

  @Column(name = "circular_id", nullable = false)
  private UUID circularId;

  @Column(name = "user_id", nullable = false)
  private String userId;
}
