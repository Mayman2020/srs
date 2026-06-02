package com.gov.ac.feature.sla.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import com.gov.ac.feature.sla.repository.SlaPolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pins the specificity-ranking contract of {@link SlaPolicyResolverService}. The job behaviour
 * downstream depends entirely on this ordering, so every change to the matcher must be matched
 * by a failing test here before it can ship.
 */
@ExtendWith(MockitoExtension.class)
class SlaPolicyResolverServiceTest {

  @Mock private SlaPolicyRepository slaPolicyRepository;

  @InjectMocks private SlaPolicyResolverService resolver;

  private SlaPolicyEntity defaultPolicy;
  private SlaPolicyEntity priorityOnly;
  private SlaPolicyEntity priorityAndConfidentiality;
  private SlaPolicyEntity differentCorrespondenceType;

  @BeforeEach
  void setUp() {
    defaultPolicy = policy(1L, "SLA_DEFAULT", null, null, null, null);
    priorityOnly = policy(2L, "SLA_URGENT", null, priority(10L), null, null);
    priorityAndConfidentiality =
        policy(3L, "SLA_URGENT_RESTRICTED", null, priority(10L), confidentiality(20L), null);
    differentCorrespondenceType =
        policy(4L, "SLA_INBOUND", correspondenceType(30L), null, null, null);
  }

  @Test
  void resolvesDefaultWhenNoCriteriaMatch() {
    when(slaPolicyRepository.findByActiveTrueAndDeletedAtIsNull())
        .thenReturn(List.of(defaultPolicy, priorityOnly));

    Optional<SlaPolicyEntity> resolved = resolver.resolveFor(null, 99L, null, null, null);

    // Priority criterion 10 != 99, so only the wildcard SLA_DEFAULT matches.
    assertThat(resolved).contains(defaultPolicy);
  }

  @Test
  void prefersHigherSpecificityWhenSeveralMatch() {
    when(slaPolicyRepository.findByActiveTrueAndDeletedAtIsNull())
        .thenReturn(List.of(defaultPolicy, priorityOnly, priorityAndConfidentiality));

    Optional<SlaPolicyEntity> resolved = resolver.resolveFor(null, 10L, 20L, null, null);

    // All three match; priorityAndConfidentiality has the highest specificity (2 criteria).
    assertThat(resolved).contains(priorityAndConfidentiality);
  }

  @Test
  void rejectsPolicyWhenAnyNonNullCriterionDoesNotMatch() {
    when(slaPolicyRepository.findByActiveTrueAndDeletedAtIsNull())
        .thenReturn(List.of(differentCorrespondenceType, defaultPolicy));

    Optional<SlaPolicyEntity> resolved = resolver.resolveFor(99L, null, null, null, null);

    // correspondence_type criterion 30 != 99 on differentCorrespondenceType, so default wins.
    assertThat(resolved).contains(defaultPolicy);
  }

  @Test
  void returnsEmptyWhenNoPoliciesActive() {
    when(slaPolicyRepository.findByActiveTrueAndDeletedAtIsNull()).thenReturn(List.of());

    assertThat(resolver.resolveFor(null, null, null, null, null)).isEmpty();
  }

  @Test
  void orgLevelCodeIsMatchedCaseInsensitively() {
    SlaPolicyEntity sLevel = new SlaPolicyEntity();
    sLevel.setId(5L);
    sLevel.setCode("SLA_S_LEVEL");
    sLevel.setOrgLevelCode("S");
    sLevel.setTargetHours(8);
    sLevel.setActive(true);

    when(slaPolicyRepository.findByActiveTrueAndDeletedAtIsNull())
        .thenReturn(List.of(defaultPolicy, sLevel));

    Optional<SlaPolicyEntity> resolved = resolver.resolveFor(null, null, null, "s", null);

    assertThat(resolved).contains(sLevel);
  }

  // ---------------------------------------------------------------------------
  // factories
  // ---------------------------------------------------------------------------

  private static SlaPolicyEntity policy(
      Long id,
      String code,
      CorrespondenceTypeEntity type,
      PriorityEntity priority,
      ConfidentialityEntity confidentiality,
      String orgLevelCode) {
    SlaPolicyEntity p = new SlaPolicyEntity();
    p.setId(id);
    p.setCode(code);
    p.setNameAr(code);
    p.setNameEn(code);
    p.setActive(true);
    p.setTargetHours(24);
    p.setBreachGraceMinutes(0);
    p.setCorrespondenceType(type);
    p.setPriority(priority);
    p.setConfidentiality(confidentiality);
    p.setOrgLevelCode(orgLevelCode);
    return p;
  }

  private static CorrespondenceTypeEntity correspondenceType(Long id) {
    CorrespondenceTypeEntity e = new CorrespondenceTypeEntity();
    e.setId(id);
    e.setCode("CT_" + id);
    e.setNameAr("type-" + id);
    e.setNameEn("type-" + id);
    return e;
  }

  private static PriorityEntity priority(Long id) {
    PriorityEntity e = new PriorityEntity();
    e.setId(id);
    e.setCode("P_" + id);
    e.setNameAr("priority-" + id);
    e.setNameEn("priority-" + id);
    return e;
  }

  private static ConfidentialityEntity confidentiality(Long id) {
    ConfidentialityEntity e = new ConfidentialityEntity();
    e.setId(id);
    e.setCode("C_" + id);
    e.setNameAr("c-" + id);
    e.setNameEn("c-" + id);
    return e;
  }
}
