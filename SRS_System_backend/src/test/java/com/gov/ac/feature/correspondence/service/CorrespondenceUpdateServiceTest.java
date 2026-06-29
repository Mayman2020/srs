package com.gov.ac.feature.correspondence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.correspondence.dto.CorrespondenceDetailResponseDto;
import com.gov.ac.feature.correspondence.dto.CorrespondencePatchRequestDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.organizations.repository.OrganizationRepository;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CorrespondenceUpdateServiceTest {

  @Mock private CorrespondenceRepository correspondenceRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private OrganizationRepository organizationRepository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private CorrespondenceViewAuthorization correspondenceViewAuthorization;
  @Mock private LookupResolutionService lookups;
  @Mock private CorrespondenceDetailService correspondenceDetailService;
  @Mock private CorrespondenceActionAudit correspondenceActionAudit;

  @InjectMocks private CorrespondenceUpdateService service;

  private UUID correspondenceId;
  private UUID actorUserId;
  private CorrespondenceEntity correspondence;
  private AppUserEntity actor;

  @BeforeEach
  void setUp() {
    correspondenceId = UUID.randomUUID();
    actorUserId = UUID.randomUUID();

    CorrespondenceStatusEntity status = new CorrespondenceStatusEntity();
    status.setCode("IN_PROGRESS");
    status.setTerminal(false);

    correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);
    correspondence.setSubject("Old subject");
    correspondence.setCorrespondenceStatus(status);

    actor = new AppUserEntity();
    actor.setId(actorUserId);
    actor.setActive(true);
  }

  @Test
  void patchSubjectUpdatesAndReturnsDetail() {
    when(appUserRepository.findByIdAndDeletedAtIsNull(actorUserId)).thenReturn(Optional.of(actor));
    when(correspondenceRepository.findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId))
        .thenReturn(Optional.of(correspondence));
    when(correspondenceRepository.save(any(CorrespondenceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CorrespondenceDetailResponseDto detail =
        CorrespondenceDetailResponseDto.builder().id(correspondenceId).subject("New subject").build();
    when(correspondenceDetailService.getById(correspondenceId, actorUserId)).thenReturn(detail);

    CorrespondenceDetailResponseDto result =
        service.patch(
            correspondenceId,
            actorUserId,
            new CorrespondencePatchRequestDto(
                "New subject",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

    assertThat(result.getSubject()).isEqualTo("New subject");
    assertThat(correspondence.getSubject()).isEqualTo("New subject");
    verify(correspondenceActionAudit)
        .log(eq(actorUserId), eq(CorrespondenceUpdateService.ACTION_UPDATED), eq(correspondenceId), any());
  }

  @Test
  void patchWithNoFieldsThrowsBadRequest() {
    when(appUserRepository.findByIdAndDeletedAtIsNull(actorUserId)).thenReturn(Optional.of(actor));
    when(correspondenceRepository.findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId))
        .thenReturn(Optional.of(correspondence));

    assertThatThrownBy(
            () ->
                service.patch(
                    correspondenceId,
                    actorUserId,
                    new CorrespondencePatchRequestDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No updatable fields");
  }
}
