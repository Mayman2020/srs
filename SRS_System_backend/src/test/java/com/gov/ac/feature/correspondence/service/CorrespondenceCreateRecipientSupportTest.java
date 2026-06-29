package com.gov.ac.feature.correspondence.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreateFormDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreateUserRecipientFormDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientKindEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientKindRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceUserRecipientRepository;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
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

@ExtendWith(MockitoExtension.class)
class CorrespondenceCreateRecipientSupportTest {

  @Mock private CorrespondenceRecipientRepository correspondenceRecipientRepository;
  @Mock private CorrespondenceUserRecipientRepository correspondenceUserRecipientRepository;
  @Mock private CorrespondenceRecipientKindRepository correspondenceRecipientKindRepository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private AppUserRepository appUserRepository;

  @InjectMocks private CorrespondenceCreateRecipientSupport support;

  @Test
  void persistAfterCreate_savesDepartmentAndUserRecipients() {
    UUID actor = UUID.randomUUID();
    CorrespondenceEntity correspondence = new CorrespondenceEntity();
    correspondence.setId(UUID.randomUUID());

    CorrespondenceCreateFormDto form = new CorrespondenceCreateFormDto();
    form.setRecipientDepartmentIds(List.of(5L));
    form.setCcDepartmentIds(List.of(6L));
    form.setUserRecipients(
        List.of(new CorrespondenceCreateUserRecipientFormDto(UUID.randomUUID(), "TO")));

    DepartmentEntity dept5 = new DepartmentEntity();
    dept5.setId(5L);
    DepartmentEntity dept6 = new DepartmentEntity();
    dept6.setId(6L);
    when(departmentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(dept5));
    when(departmentRepository.findByIdAndDeletedAtIsNull(6L)).thenReturn(Optional.of(dept6));
    when(correspondenceRecipientRepository.existsActivePair(any(), any(Long.class)))
        .thenReturn(false);

    CorrespondenceRecipientKindEntity kind = new CorrespondenceRecipientKindEntity();
    kind.setId(1L);
    when(correspondenceRecipientKindRepository.findActiveByCode("TO")).thenReturn(Optional.of(kind));

    AppUserEntity user = new AppUserEntity();
    user.setId(form.getUserRecipients().get(0).recipientUserId());
    user.setActive(true);
    when(appUserRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
    when(correspondenceUserRecipientRepository.existsActiveTriple(
            correspondence.getId(), user.getId(), kind.getId()))
        .thenReturn(false);

    support.persistAfterCreate(actor, correspondence, form);

    verify(correspondenceRecipientRepository, org.mockito.Mockito.times(2)).save(any());
    verify(correspondenceUserRecipientRepository).save(any());
  }

  @Test
  void persistAfterCreate_rejectsUnknownDepartment() {
    CorrespondenceEntity correspondence = new CorrespondenceEntity();
    correspondence.setId(UUID.randomUUID());
    CorrespondenceCreateFormDto form = new CorrespondenceCreateFormDto();
    form.setRecipientDepartmentIds(List.of(99L));
    when(departmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> support.persistAfterCreate(UUID.randomUUID(), correspondence, form))
        .isInstanceOf(BadRequestException.class);

    verify(correspondenceUserRecipientRepository, never()).save(any());
  }
}
