package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves which {@link WorkflowActionTypeEntity} row applies for a decision code and current status. */
@Service
@RequiredArgsConstructor
public class WorkflowActionResolutionService {

  private final WorkflowActionTypeRepository workflowActionTypeRepository;

  public Optional<WorkflowActionTypeEntity> resolveTransition(String code, Long fromCorrespondenceStatusId) {
    if (fromCorrespondenceStatusId == null) {
      return resolveWildcardOnly(code);
    }
    List<WorkflowActionTypeEntity> rows =
        workflowActionTypeRepository.findRulesMatchingCodeAndStatus(code, fromCorrespondenceStatusId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  /** Rows with {@code allowed_from_correspondence_status_id IS NULL} only (CREATE, CLOSE, …). */
  public Optional<WorkflowActionTypeEntity> resolveWildcardOnly(String code) {
    List<WorkflowActionTypeEntity> rows = workflowActionTypeRepository.findWildcardRulesForCode(code);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  /**
   * When multiple rows share the same {@code code} (wildcard + status-specific), keep the most
   * specific rule.
   */
  public static List<WorkflowActionTypeEntity> dedupeByCodePreferSpecific(List<WorkflowActionTypeEntity> rows) {
    java.util.Map<String, WorkflowActionTypeEntity> best = new java.util.LinkedHashMap<>();
    for (WorkflowActionTypeEntity w : rows) {
      String key = w.getCode().toUpperCase(Locale.ROOT);
      WorkflowActionTypeEntity prev = best.get(key);
      if (prev == null) {
        best.put(key, w);
      } else if (prev.getAllowedFromCorrespondenceStatus() == null
          && w.getAllowedFromCorrespondenceStatus() != null) {
        best.put(key, w);
      }
    }
    return best.values().stream()
        .sorted(Comparator.comparing(WorkflowActionTypeEntity::getSortOrder).thenComparing(WorkflowActionTypeEntity::getId))
        .toList();
  }
}
