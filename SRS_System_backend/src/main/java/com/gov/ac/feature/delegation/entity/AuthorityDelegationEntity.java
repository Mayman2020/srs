package com.gov.ac.feature.delegation.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "authority_delegation", schema = "srs_system")
@Getter
@Setter
public class AuthorityDelegationEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "delegator_user_id", nullable = false)
  private AppUserEntity delegatorUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "delegate_user_id", nullable = false)
  private AppUserEntity delegateUser;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = false)
  private LocalDate validTo;

  @Column(name = "allowed_correspondence_type_codes", columnDefinition = "text")
  private String allowedCorrespondenceTypeCodes;

  @Column(name = "allowed_confidentiality_codes", columnDefinition = "text")
  private String allowedConfidentialityCodes;

  @Column(name = "can_sign_on_behalf", nullable = false)
  private Boolean canSignOnBehalf = false;

  @Column(columnDefinition = "text")
  private String notes;
}
