package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowActionTypeRepository extends JpaRepository<WorkflowActionTypeEntity, Long> {

  List<WorkflowActionTypeEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<WorkflowActionTypeEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<WorkflowActionTypeEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);

  /**
   * Rules for {@code code} matching current status: prefer a row with explicit {@code
   * allowed_from_correspondence_status} over a wildcard row.
   */
  @Query(
      """
      SELECT w FROM WorkflowActionTypeEntity w
      WHERE w.deletedAt IS NULL AND w.active = true
      AND UPPER(TRIM(w.code)) = UPPER(TRIM(:code))
      AND (
        w.allowedFromCorrespondenceStatus IS NULL
        OR w.allowedFromCorrespondenceStatus.id = :fromStatusId
      )
      ORDER BY CASE WHEN w.allowedFromCorrespondenceStatus IS NOT NULL THEN 0 ELSE 1 END, w.id ASC
      """)
  List<WorkflowActionTypeEntity> findRulesMatchingCodeAndStatus(
      @Param("code") String code, @Param("fromStatusId") Long fromStatusId);

  /** Wildcard rows only ({@code allowed_from} is null) — e.g. CREATE, CLOSE catalog entries. */
  @Query(
      """
      SELECT w FROM WorkflowActionTypeEntity w
      WHERE w.deletedAt IS NULL AND w.active = true
      AND UPPER(TRIM(w.code)) = UPPER(TRIM(:code))
      AND w.allowedFromCorrespondenceStatus IS NULL
      ORDER BY w.id ASC
      """)
  List<WorkflowActionTypeEntity> findWildcardRulesForCode(@Param("code") String code);

  /**
   * Task-decision actions available for a lifecycle status and role set. Pass {@code roleIdsSentinel}
   * as {@code [-1]} when the user has no roles so that {@code required_role_id} rows never match.
   */
  @Query(
      """
      SELECT w FROM WorkflowActionTypeEntity w
      WHERE w.deletedAt IS NULL AND w.active = true AND w.showInTaskDecisionUi = true
      AND (
        w.allowedFromCorrespondenceStatus IS NULL
        OR w.allowedFromCorrespondenceStatus.id = :statusId
      )
      AND (
        w.requiredRole IS NULL OR w.requiredRole.id IN :roleIds
      )
      ORDER BY w.sortOrder ASC, w.id ASC
      """)
  List<WorkflowActionTypeEntity> findTaskDecisionActionsForStatusAndRoles(
      @Param("statusId") Long statusId, @Param("roleIds") List<Long> roleIds);
}
