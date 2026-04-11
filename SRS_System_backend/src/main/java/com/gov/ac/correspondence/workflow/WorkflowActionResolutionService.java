package com.gov.ac.correspondence.workflow;

import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.persistence.WorkflowActionTypeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves which {@link WorkflowActionType} row applies for a decision code and current status. */
@Service
@RequiredArgsConstructor
public class WorkflowActionResolutionService {

  private final WorkflowActionTypeRepository workflowActionTypeRepository;

  public Optional<WorkflowActionType> resolveTransition(String code, Long fromCorrespondenceStatusId) {
    if (fromCorrespondenceStatusId == null) {
      return resolveWildcardOnly(code);
    }
    List<WorkflowActionType> rows =
        workflowActionTypeRepository.findRulesMatchingCodeAndStatus(code, fromCorrespondenceStatusId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  /** Rows with {@code allowed_from_correspondence_status_id IS NULL} only (CREATE, CLOSE, …). */
  public Optional<WorkflowActionType> resolveWildcardOnly(String code) {
    List<WorkflowActionType> rows = workflowActionTypeRepository.findWildcardRulesForCode(code);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  /**
   * When multiple rows share the same {@code code} (wildcard + status-specific), keep the most
   * specific rule.
   */
  public static List<WorkflowActionType> dedupeByCodePreferSpecific(List<WorkflowActionType> rows) {
    java.util.Map<String, WorkflowActionType> best = new java.util.LinkedHashMap<>();
    for (WorkflowActionType w : rows) {
      String key = w.getCode().toUpperCase(Locale.ROOT);
      WorkflowActionType prev = best.get(key);
      if (prev == null) {
        best.put(key, w);
      } else if (prev.getAllowedFromCorrespondenceStatus() == null
          && w.getAllowedFromCorrespondenceStatus() != null) {
        best.put(key, w);
      }
    }
    return best.values().stream()
        .sorted(Comparator.comparing(WorkflowActionType::getSortOrder).thenComparing(WorkflowActionType::getId))
        .toList();
  }
}
