package com.gov.ac.feature.acting.service;

import com.gov.ac.feature.acting.entity.ActingAssignmentEntity;
import com.gov.ac.feature.acting.repository.ActingAssignmentRepository;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Picks the highest-specificity {@link ActingAssignmentEntity} that matches the current task
 * context. NULL criterion columns act as wildcards. Tie-breaker: lowest UUID for stability.
 *
 * <p>Precedence (Slice 4): this resolver runs <strong>after</strong> the workflow listener has
 * computed the direct assignee and <strong>before</strong> task delegation rewires the task, so
 * the acting overlay substitutes the acting user for the absent user without breaking the
 * delegation chain on top.
 */
@Component
@RequiredArgsConstructor
public class ActingAssignmentMatcher {

  private final ActingAssignmentRepository actingAssignmentRepository;

  public Optional<ActingAssignmentEntity> findBestMatch(
      UUID absentUserId,
      CorrespondenceEntity correspondence,
      String processDefinitionKey,
      String taskDefinitionKey,
      Long workflowActionTypeId) {
    if (absentUserId == null) {
      return Optional.empty();
    }
    LocalDate today = LocalDate.now();
    List<ActingAssignmentEntity> rows =
        actingAssignmentRepository.findActiveByAbsentUser(absentUserId, today);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return rows.stream()
        .filter(r -> matchesDepartment(r, correspondence))
        .filter(r -> matchesOrgLevel(r, correspondence))
        .filter(r -> matchesCorrespondenceType(r, correspondence))
        .filter(r -> matchesConfidentiality(r, correspondence))
        .filter(r -> matchesProcessKey(r, processDefinitionKey))
        .filter(r -> matchesTaskDefinitionKey(r, taskDefinitionKey))
        .filter(r -> matchesWorkflowActionType(r, workflowActionTypeId))
        .max(
            Comparator.comparingInt(ActingAssignmentEntity::specificityScore)
                .thenComparing(ActingAssignmentEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())));
  }

  private boolean matchesDepartment(ActingAssignmentEntity row, CorrespondenceEntity c) {
    if (row.getDepartment() == null) {
      return true;
    }
    if (c == null || c.getOwnerDepartment() == null) {
      return false;
    }
    Long scopeDeptId = row.getDepartment().getId();
    DepartmentEntity owner = c.getOwnerDepartment();
    if (owner.getId().equals(scopeDeptId)) {
      return true;
    }
    if (!row.isIncludeDepartmentSubtree()) {
      return false;
    }
    DepartmentEntity cur = owner.getParent();
    while (cur != null) {
      if (cur.getId().equals(scopeDeptId)) {
        return true;
      }
      cur = cur.getParent();
    }
    return false;
  }

  private boolean matchesOrgLevel(ActingAssignmentEntity row, CorrespondenceEntity c) {
    if (!StringUtils.hasText(row.getOrgLevelCode())) {
      return true;
    }
    if (c == null || c.getOwnerDepartment() == null) {
      return false;
    }
    String lvl = c.getOwnerDepartment().getLevelCode();
    return row.getOrgLevelCode().trim().equalsIgnoreCase(lvl != null ? lvl.trim() : "");
  }

  private boolean matchesCorrespondenceType(ActingAssignmentEntity row, CorrespondenceEntity c) {
    if (row.getCorrespondenceType() == null) {
      return true;
    }
    if (c == null || c.getCorrespondenceType() == null) {
      return false;
    }
    return row.getCorrespondenceType().getId().equals(c.getCorrespondenceType().getId());
  }

  private boolean matchesConfidentiality(ActingAssignmentEntity row, CorrespondenceEntity c) {
    if (row.getConfidentiality() == null) {
      return true;
    }
    if (c == null || c.getConfidentiality() == null) {
      return false;
    }
    return row.getConfidentiality().getId().equals(c.getConfidentiality().getId());
  }

  private boolean matchesProcessKey(ActingAssignmentEntity row, String processDefinitionKey) {
    if (!StringUtils.hasText(row.getProcessDefinitionKey())) {
      return true;
    }
    if (!StringUtils.hasText(processDefinitionKey)) {
      return false;
    }
    return row.getProcessDefinitionKey().trim().equalsIgnoreCase(processDefinitionKey.trim());
  }

  private boolean matchesTaskDefinitionKey(ActingAssignmentEntity row, String taskDefinitionKey) {
    if (!StringUtils.hasText(row.getTaskDefinitionKey())) {
      return true;
    }
    if (!StringUtils.hasText(taskDefinitionKey)) {
      return false;
    }
    return row.getTaskDefinitionKey().trim().equals(taskDefinitionKey.trim());
  }

  private boolean matchesWorkflowActionType(ActingAssignmentEntity row, Long workflowActionTypeId) {
    if (row.getWorkflowActionType() == null) {
      return true;
    }
    if (workflowActionTypeId == null) {
      return false;
    }
    return row.getWorkflowActionType().getId().equals(workflowActionTypeId);
  }
}
