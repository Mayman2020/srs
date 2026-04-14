package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.dto.WorkflowActionAvailableDto;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceCamundaTaskSupport;
import com.gov.ac.feature.correspondence.workflow.WorkflowActionResolutionService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CorrespondenceWorkflowAvailabilityService {

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final UserRoleRepository userRoleRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final CorrespondenceCamundaTaskSupport camundaTaskSupport;
  private final WorkflowActionResolutionService workflowActionResolutionService;

  @Transactional(readOnly = true)
  public List<WorkflowActionAvailableDto> listAvailableWorkflowActions(
      UUID correspondenceId, UUID viewerId) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(() -> new ForbiddenException("You cannot perform this action"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You cannot perform this action");
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    if (correspondence.getDeletedAt() != null) {
      throw new NotFoundException("CorrespondenceEntity not found");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    if (correspondence.getCorrespondenceStatus() == null) {
      return List.of();
    }
    if (Boolean.TRUE.equals(correspondence.getCorrespondenceStatus().getTerminal())) {
      return List.of();
    }

    String businessKey = correspondence.getReferenceNumber();
    if (!StringUtils.hasText(businessKey)) {
      return List.of();
    }

    if (camundaTaskSupport.findActiveTasksForUser(businessKey, viewerId).isEmpty()) {
      return List.of();
    }

    Long statusId = correspondence.getCorrespondenceStatus().getId();
    List<Long> roleIds = userRoleRepository.findActiveRoleIdsByUserId(viewerId);
    if (roleIds.isEmpty()) {
      roleIds = List.of(-1L);
    }

    List<WorkflowActionTypeEntity> raw =
        workflowActionTypeRepository.findTaskDecisionActionsForStatusAndRoles(statusId, roleIds);
    List<WorkflowActionTypeEntity> deduped = workflowActionResolutionService.dedupeByCodePreferSpecific(raw);

    return deduped.stream()
        .map(
            w ->
                new WorkflowActionAvailableDto(
                    w.getId(),
                    w.getCode(),
                    w.getNameAr(),
                    w.getNameEn(),
                    Boolean.TRUE.equals(w.getRequiresComment()),
                    w.getSortOrder() != null ? w.getSortOrder() : 0,
                    w.getUiVariant() != null ? w.getUiVariant() : "secondary"))
        .toList();
  }
}
