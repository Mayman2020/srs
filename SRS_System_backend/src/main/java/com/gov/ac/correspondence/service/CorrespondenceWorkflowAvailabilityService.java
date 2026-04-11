package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.dto.WorkflowActionAvailableDto;
import com.gov.ac.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.correspondence.workflow.CorrespondenceCamundaTaskSupport;
import com.gov.ac.correspondence.workflow.WorkflowActionResolutionService;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.persistence.UserRoleRepository;
import com.gov.ac.persistence.WorkflowActionTypeRepository;
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
    AppUser viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(() -> new ForbiddenException("You cannot perform this action"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You cannot perform this action");
    }

    Correspondence correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("Correspondence not found"));
    if (correspondence.getDeletedAt() != null) {
      throw new NotFoundException("Correspondence not found");
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

    List<WorkflowActionType> raw =
        workflowActionTypeRepository.findTaskDecisionActionsForStatusAndRoles(statusId, roleIds);
    List<WorkflowActionType> deduped = workflowActionResolutionService.dedupeByCodePreferSpecific(raw);

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
