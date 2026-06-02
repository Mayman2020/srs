package com.gov.ac.feature.sla.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Confidentiality boundary: an SLA escalation action must never widen the audience past the
 * clearance of the underlying correspondence. The filter mirrors {@code CorrespondenceViewAuthorization}
 * (lower {@code sort_order} = more restrictive; user must satisfy {@code user.sortOrder <=
 * correspondence.sortOrder}).
 */
@ExtendWith(MockitoExtension.class)
class SlaClearanceFilterTest {

  @Mock private AppUserRepository appUserRepository;
  @Mock private ConfidentialityRepository confidentialityRepository;

  @InjectMocks private SlaClearanceFilter filter;

  @Test
  void unrestrictedCorrespondenceLetsEverybodyThrough() {
    CorrespondenceEntity unrestricted = correspondenceWithConfidentiality(null, false);

    List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

    assertThat(filter.filter(unrestricted, ids)).containsExactlyInAnyOrderElementsOf(ids);
  }

  @Test
  void restrictedCorrespondenceFiltersOutUsersWithoutClearance() {
    UUID cleared = UUID.randomUUID();
    UUID noClearance = UUID.randomUUID();
    CorrespondenceEntity restricted = correspondenceWithConfidentiality(10, true);

    AppUserEntity clearedUser = userWithClearance(cleared, 10L);
    AppUserEntity uncleared = userWithClearance(noClearance, null);

    when(appUserRepository.findByIdAndDeletedAtIsNull(cleared)).thenReturn(Optional.of(clearedUser));
    when(appUserRepository.findByIdAndDeletedAtIsNull(noClearance))
        .thenReturn(Optional.of(uncleared));
    lenient()
        .when(confidentialityRepository.findByIdAndDeletedAtIsNull(10L))
        .thenReturn(Optional.of(confidentialityRow(10L, 10)));

    List<UUID> allowed = filter.filter(restricted, List.of(cleared, noClearance));

    assertThat(allowed).containsExactly(cleared);
  }

  @Test
  void restrictedCorrespondenceRejectsLowerClearanceUser() {
    UUID lower = UUID.randomUUID();
    CorrespondenceEntity restricted = correspondenceWithConfidentiality(10, true);

    AppUserEntity lowerUser = userWithClearance(lower, 50L);
    when(appUserRepository.findByIdAndDeletedAtIsNull(lower)).thenReturn(Optional.of(lowerUser));
    when(confidentialityRepository.findByIdAndDeletedAtIsNull(50L))
        .thenReturn(Optional.of(confidentialityRow(50L, 50)));

    assertThat(filter.filter(restricted, List.of(lower))).isEmpty();
  }

  // ---------------------------------------------------------------------------

  private static CorrespondenceEntity correspondenceWithConfidentiality(
      Integer sortOrder, boolean requires) {
    CorrespondenceEntity c = new CorrespondenceEntity();
    c.setId(UUID.randomUUID());
    if (sortOrder == null) {
      return c;
    }
    ConfidentialityEntity level = confidentialityRow(99L, sortOrder);
    level.setRequiresClearance(requires);
    c.setConfidentiality(level);
    return c;
  }

  private static ConfidentialityEntity confidentialityRow(Long id, int sortOrder) {
    ConfidentialityEntity e = new ConfidentialityEntity();
    e.setId(id);
    e.setCode("L-" + sortOrder);
    e.setNameAr("l");
    e.setNameEn("l");
    e.setSortOrder(sortOrder);
    e.setRequiresClearance(false);
    return e;
  }

  private static AppUserEntity userWithClearance(UUID id, Long clearanceId) {
    AppUserEntity u = new AppUserEntity();
    u.setId(id);
    u.setSecurityClearanceId(clearanceId);
    return u;
  }
}
