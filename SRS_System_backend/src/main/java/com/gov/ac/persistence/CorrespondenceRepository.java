package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.Correspondence;
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
    extends JpaRepository<Correspondence, UUID>, JpaSpecificationExecutor<Correspondence> {

  @EntityGraph(value = "Correspondence.list", type = EntityGraphType.LOAD)
  Page<Correspondence> findAll(
      @Nullable Specification<Correspondence> spec, Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "correspondenceType",
        "correspondenceStatus",
        "priority",
        "confidentiality",
        "classification",
        "senderOrganization",
        "recipientOrganization",
        "ownerDepartment"
      })
  @Query("select c from Correspondence c where c.id = :id and c.deletedAt is null")
  Optional<Correspondence> findDetailGraphByIdAndDeletedAtIsNull(@Param("id") UUID id);

  @EntityGraph(attributePaths = {"ownerDepartment", "correspondenceType"})
  @Query("select c from Correspondence c where c.id = :id and c.deletedAt is null")
  Optional<Correspondence> findByIdAndDeletedAtIsNullWithOwnerDepartment(@Param("id") UUID id);

  @Query("select count(c) from Correspondence c where c.deletedAt is null")
  long countActive();

  /**
   * One row per {@link com.gov.ac.domain.lookup.CorrespondenceStatus}: id, code, nameAr, nameEn,
   * sortOrder, count — all label fields from the lookup table.
   */
  @Query(
      "select s.id, s.code, s.nameAr, s.nameEn, s.sortOrder, count(c) "
          + "from Correspondence c join c.correspondenceStatus s "
          + "where c.deletedAt is null and s.deletedAt is null "
          + "group by s.id, s.code, s.nameAr, s.nameEn, s.sortOrder "
          + "order by s.sortOrder")
  List<Object[]> aggregateActiveByCorrespondenceStatus();

  /**
   * One row per {@link com.gov.ac.domain.lookup.Priority}: id, code, nameAr, nameEn, sortOrder,
   * count.
   */
  @Query(
      "select p.id, p.code, p.nameAr, p.nameEn, p.sortOrder, count(c) "
          + "from Correspondence c join c.priority p "
          + "where c.deletedAt is null and p.deletedAt is null "
          + "group by p.id, p.code, p.nameAr, p.nameEn, p.sortOrder "
          + "order by p.sortOrder")
  List<Object[]> aggregateActiveByPriority();

  /**
   * Items with a due date in the past and a non-terminal lifecycle status (still actionable).
   */
  @Query(
      "select count(c) from Correspondence c join c.correspondenceStatus s "
          + "where c.deletedAt is null and s.deletedAt is null and s.terminal = false "
          + "and c.dueDate is not null and c.dueDate < :now")
  long countOverdueOpen(@Param("now") Instant now);

  @Query(
      value =
          "select date_trunc('month', c.created_at), count(*) "
              + "from correspondence c "
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
              + "from correspondence c "
              + "join department d on d.id = c.owner_department_id and d.deleted_at is null "
              + "join correspondence_status s on s.id = c.correspondence_status_id and s.deleted_at is null "
              + "where c.deleted_at is null and c.owner_department_id is not null "
              + "group by d.id, d.code, d.name_ar, d.name_en, d.sort_order "
              + "order by d.sort_order, d.id",
      nativeQuery = true)
  List<Object[]> departmentSlaHeatmap(@Param("now") Instant now);
}
