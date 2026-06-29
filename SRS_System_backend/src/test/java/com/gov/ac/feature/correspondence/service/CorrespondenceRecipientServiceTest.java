package com.gov.ac.feature.correspondence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.correspondence.dto.CorrespondenceRecipientDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceUserRecipientDto;
import com.gov.ac.feature.correspondence.dto.UpsertCorrespondenceRecipientRequestDto;
import com.gov.ac.feature.correspondence.dto.UpsertCorrespondenceUserRecipientRequestDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientKindEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceUserRecipientEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientKindRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceUserRecipientRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CorrespondenceRecipientServiceTest {

  @Mock private CorrespondenceRepository correspondenceRepository;
  @Mock private CorrespondenceRecipientRepository correspondenceRecipientRepository;
  @Mock private CorrespondenceUserRecipientRepository correspondenceUserRecipientRepository;
  @Mock private CorrespondenceRecipientKindRepository correspondenceRecipientKindRepository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private CorrespondenceViewAuthorization correspondenceViewAuthorization;

  @InjectMocks private CorrespondenceRecipientService service;

  private UUID correspondenceId;
  private UUID viewerId;
  private CorrespondenceEntity correspondence;
  private AppUserEntity viewer;

  @BeforeEach
  void setUp() {
    correspondenceId = UUID.randomUUID();
    viewerId = UUID.randomUUID();

    CorrespondenceStatusEntity status = new CorrespondenceStatusEntity();
    status.setCode("IN_PROGRESS");
    status.setTerminal(false);

    correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);
    correspondence.setCorrespondenceStatus(status);

    viewer = new AppUserEntity();
    viewer.setId(viewerId);
    viewer.setActive(true);
  }

  @Test
  void listReturnsMappedDtos() {
    stubViewAccess();

    DepartmentEntity dept = new DepartmentEntity();
    dept.setId(10L);
    dept.setCode("DEPT-A");
    dept.setNameAr("إدارة أ");
    dept.setNameEn("Dept A");

    CorrespondenceRecipientEntity row = new CorrespondenceRecipientEntity();
    row.setId(1L);
    row.setCorrespondence(correspondence);
    row.setDepartment(dept);

    when(correspondenceRecipientRepository.listActiveForCorrespondence(correspondenceId))
        .thenReturn(List.of(row));

    List<CorrespondenceRecipientDto> result = service.list(correspondenceId, viewerId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).departmentCode()).isEqualTo("DEPT-A");
  }

  @Test
  void addDuplicateDepartmentThrowsBadRequest() {
    stubMutableAccess();
    when(correspondenceRecipientRepository.existsActivePair(correspondenceId, 10L)).thenReturn(true);

    assertThatThrownBy(
            () -> service.add(correspondenceId, viewerId, new UpsertCorrespondenceRecipientRequestDto(10L)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already a recipient");
  }

  @Test
  void addUserRecipientPersistsRow() {
    stubMutableAccess();

    CorrespondenceRecipientKindEntity kind = new CorrespondenceRecipientKindEntity();
    kind.setId(2L);
    kind.setCode("TO");

    AppUserEntity recipient = new AppUserEntity();
    recipient.setId(UUID.randomUUID());
    recipient.setUsername("user1");
    recipient.setFullNameAr("مستخدم");
    recipient.setFullNameEn("User");
    recipient.setActive(true);

    when(correspondenceRecipientKindRepository.findActiveByCode("TO")).thenReturn(Optional.of(kind));
    when(appUserRepository.findByIdAndDeletedAtIsNull(recipient.getId())).thenReturn(Optional.of(recipient));
    when(correspondenceUserRecipientRepository.existsActiveTriple(correspondenceId, recipient.getId(), 2L))
        .thenReturn(false);
    when(correspondenceUserRecipientRepository.save(any(CorrespondenceUserRecipientEntity.class)))
        .thenAnswer(
            inv -> {
              CorrespondenceUserRecipientEntity saved = inv.getArgument(0);
              saved.setId(99L);
              return saved;
            });

    CorrespondenceUserRecipientDto dto =
        service.addUserRecipient(
            correspondenceId,
            viewerId,
            new UpsertCorrespondenceUserRecipientRequestDto(recipient.getId(), "TO"));

    assertThat(dto.recipientKindCode()).isEqualTo("TO");
    assertThat(dto.recipientUsername()).isEqualTo("user1");
    verify(correspondenceUserRecipientRepository).save(any(CorrespondenceUserRecipientEntity.class));
  }

  private void stubViewAccess() {
    when(correspondenceRepository.findDetailGraphByIdAndDeletedAtIsNull(correspondenceId))
        .thenReturn(Optional.of(correspondence));
    when(appUserRepository.findByIdAndDeletedAtIsNull(viewerId)).thenReturn(Optional.of(viewer));
  }

  private void stubMutableAccess() {
    stubViewAccess();
  }
}
