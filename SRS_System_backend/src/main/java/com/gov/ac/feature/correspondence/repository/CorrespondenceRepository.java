package com.gov.ac.feature.correspondence.repository;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceRepository
    extends JpaRepository<CorrespondenceEntity, UUID>, JpaSpecificationExecutor<CorrespondenceEntity> {

  @EntityGraph(value = "CorrespondenceEntity.list", type = EntityGraphType.LOAD)
  Page<CorrespondenceEntity> findAll(
      @Nullable Specification<CorrespondenceEntity> spec, Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "correspondenceType",
        "correspondenceStatus",
        "priority",
        "confidentiality",
        "classification",
        "senderOrganization",
        "recipientOrganization",
        "ownerDepartment",
        "serviceWorkflowRoute"
      })
  @Query("select c from CorrespondenceEntity c where c.id = :id and c.deletedAt is null")
  Optional<CorrespondenceEntity> findDetailGraphByIdAndDeletedAtIsNull(@Param("id") UUID id);

  @EntityGraph(attributePaths = {"ownerDepartment", "correspondenceType"})
  @Query("select c from CorrespondenceEntity c where c.id = :id and c.deletedAt is null")
  Optional<CorrespondenceEntity> findByIdAndDeletedAtIsNullWithOwnerDepartment(@Param("id") UUID id);

  @Query("select count(c) from CorrespondenceEntity c where c.deletedAt is null")
  long countActive();

  /**
   * Department-scoped active count. {@code departmentId == null} disables scoping (global).
   */
  @Query(
      "select count(c) from CorrespondenceEntity c "
          + "where c.deletedAt is null "
          + "and (:departmentId is null or c.ownerDepartment.id = :departmentId)")
  long countActiveScoped(@Param("departmentId") Long departmentId);

  /**
   * One row per {@link com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity}: id, code, nameAr, nameEn,
   * sortOrder, count — all label fields from the lookup table.
   */
  @Query(
      "select s.id, s.code, s.nameAr, s.nameEn, s.sortOrder, count(c) "
          + "from CorrespondenceEntity c join c.correspondenceStatus s "
          + "where c.deletedAt is null and s.deletedAt is null "
          + "and (:departmentId is null or c.ownerDepartment.id = :departmentId) "
          + "group by s.id, s.code, s.nameAr, s.nameEn, s.sortOrder "
          + "order by s.sortOrder")
  List<Object[]> aggregateActiveByCorrespondenceStatusScoped(
      @Param("departmentId") Long departmentId);

  /**
   * One row per {@link com.gov.ac.feature.lookups.entity.PriorityEntity}: id, code, nameAr, nameEn, sortOrder,
   * count.
   */
  @Query(
      "select p.id, p.code, p.nameAr, p.nameEn, p.sortOrder, count(c) "
          + "from CorrespondenceEntity c join c.priority p "
          + "where c.deletedAt is null and p.deletedAt is null "
          + "and (:departmentId is null or c.ownerDepartment.id = :departmentId) "
          + "group by p.id, p.code, p.nameAr, p.nameEn, p.sortOrder "
          + "order by p.sortOrder")
  List<Object[]> aggregateActiveByPriorityScoped(@Param("departmentId") Long departmentId);

  /**
   * Aggregation by owner-department level (Q/L/K/S). One row per active
   * {@link com.gov.ac.feature.organization.entity.OrganizationalUnitLevelEntity}: id, code,
   * nameAr, nameEn, sortOrder, count.
   */
  // organizational_unit_level uses rank_order (1..10, lower = higher authority); there is no
  // sort_order column on this table (see V5__org_levels_routing.sql). The DashboardBucketDto
  // projection slot at index 4 is the integer ordering for the bucket, so rank_order maps onto it
  // directly.
  //
  // INNER JOIN (not LEFT JOIN): we only bucket correspondences whose owner department has a
  // resolvable level. A LEFT JOIN would produce a single all-null grouping row whenever any
  // department is missing a level_code, and DashboardMapper.toBucket assumes every row has a
  // non-null id/code (matching priority/confidentiality aggregations, which also use INNER JOIN).
  @Query(
      value =
          "select lvl.id, lvl.code, lvl.name_ar, lvl.name_en, lvl.rank_order, count(c.id) "
              + "from srs_system.correspondence c "
              + "join srs_system.department d on d.id = c.owner_department_id and d.deleted_at is null "
              + "join srs_system.organizational_unit_level lvl on lvl.code = d.level_code "
              + "and lvl.deleted_at is null and lvl.is_active = true "
              + "where c.deleted_at is null "
              + "and (cast(:departmentId as bigint) is null or c.owner_department_id = :departmentId) "
              + "group by lvl.id, lvl.code, lvl.name_ar, lvl.name_en, lvl.rank_order "
              + "order by lvl.rank_order nulls last",
      nativeQuery = true)
  List<Object[]> aggregateActiveByOrgLevelScoped(@Param("departmentId") Long departmentId);

  /**
   * One row per active confidentiality level: id, code, nameAr, nameEn, sortOrder, count.
   */
  @Query(
      "select cf.id, cf.code, cf.nameAr, cf.nameEn, cf.sortOrder, count(c) "
          + "from CorrespondenceEntity c join c.confidentiality cf "
          + "where c.deletedAt is null and cf.deletedAt is null "
          + "and (:departmentId is null or c.ownerDepartment.id = :departmentId) "
          + "group by cf.id, cf.code, cf.nameAr, cf.nameEn, cf.sortOrder "
          + "order by cf.sortOrder")
  List<Object[]> aggregateActiveByConfidentialityScoped(@Param("departmentId") Long departmentId);

  /**
   * Items with a due date in the past and a non-terminal lifecycle status (still actionable).
   */
  @Query(
      "select count(c) from CorrespondenceEntity c join c.correspondenceStatus s "
          + "where c.deletedAt is null and s.deletedAt is null and s.terminal = false "
          + "and c.dueDate is not null and c.dueDate < :now "
          + "and (:departmentId is null or c.ownerDepartment.id = :departmentId)")
  long countOverdueOpenScoped(@Param("now") Instant now, @Param("departmentId") Long departmentId);

  @Query(
      "select count(c) from CorrespondenceEntity c join c.correspondenceStatus s "
          + "where c.deletedAt is null and s.deletedAt is null and s.kpiSegment = :segment "
          + "and (:departmentId is null or c.ownerDepartment.id = :departmentId)")
  long countActiveByKpiSegmentScoped(
      @Param("segment") String segment, @Param("departmentId") Long departmentId);

  @Query(
      "select count(c) from CorrespondenceEntity c join c.correspondenceType t "
          + "where c.deletedAt is null and t.deletedAt is null "
          + "and t.dashboardOutboundHighlight = true "
          + "and (:departmentId is null or c.ownerDepartment.id = :departmentId)")
  long countActiveOutboundHighlightedScoped(@Param("departmentId") Long departmentId);

  @Query(
      value =
          "select date_trunc('month', c.created_at), count(*) "
              + "from srs_system.correspondence c "
              + "where c.deleted_at is null "
              + "and c.created_at >= :fromInclusive "
              + "and c.created_at < :toExclusive "
              + "group by 1 order by 1",
      nativeQuery = true)
  List<Object[]> countCreatedByMonth(
      @Param("fromInclusive") Instant fromInclusive, @Param("toExclusive") Instant toExclusive);

  @Query(
      value =
          "select d.id, d.code, d.name_ar, d.name_en, count(c.id), "
              + "sum(case when c.due_date is not null and c.due_date < :now "
              + "and coalesce(s.is_terminal, false) = false then 1 else 0 end) "
              + "from srs_system.correspondence c "
              + "join srs_system.department d on d.id = c.owner_department_id and d.deleted_at is null "
              + "join srs_system.correspondence_status s on s.id = c.correspondence_status_id and s.deleted_at is null "
              + "where c.deleted_at is null and c.owner_department_id is not null "
              + "group by d.id, d.code, d.name_ar, d.name_en, d.sort_order "
              + "order by d.sort_order, d.id",
      nativeQuery = true)
  List<Object[]> departmentSlaHeatmap(@Param("now") Instant now);

  @Query(
      "select c.referenceNumber, c.subject, t.code, st.code, c.createdAt, c.updatedAt "
          + "from CorrespondenceEntity c join c.correspondenceType t join c.correspondenceStatus st "
          + "where c.deletedAt is null order by c.createdAt desc")
  Page<Object[]> exportRows(Pageable pageable);

  /**
   * Department-scoped export rows for restricted callers (returns reference number, subject,
   * type code, status code, confidentiality code, level code, createdAt, updatedAt).
   */
  @Query(
      value =
          "select c.reference_number, c.subject, t.code, st.code, "
              + "cf.code, d.level_code, c.created_at, c.updated_at "
              + "from srs_system.correspondence c "
              + "join srs_system.correspondence_type t on t.id = c.correspondence_type_id "
              + "join srs_system.correspondence_status st on st.id = c.correspondence_status_id "
              + "left join srs_system.confidentiality cf on cf.id = c.confidentiality_id "
              + "left join srs_system.department d on d.id = c.owner_department_id "
              + "where c.deleted_at is null "
              + "and (cast(:departmentId as bigint) is null or c.owner_department_id = :departmentId) "
              + "and (cast(:viewerSortOrder as integer) is null "
              + "  or cf.id is null "
              + "  or coalesce(cf.requires_clearance, false) = false "
              + "  or coalesce(cf.sort_order, 2147483647) >= :viewerSortOrder) "
              + "order by c.created_at desc",
      nativeQuery = true)
  Page<Object[]> exportRowsScoped(
      @Param("departmentId") Long departmentId,
      @Param("viewerSortOrder") Integer viewerSortOrder,
      Pageable pageable);
}
