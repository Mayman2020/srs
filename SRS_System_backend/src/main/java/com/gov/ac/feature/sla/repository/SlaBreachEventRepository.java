package com.gov.ac.feature.sla.repository;

import com.gov.ac.feature.sla.entity.SlaBreachEventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlaBreachEventRepository extends JpaRepository<SlaBreachEventEntity, Long> {

  Optional<SlaBreachEventEntity> findByTaskId(String taskId);

  /**
   * Unresolved breach rows newest-first. Used by the admin breach-list endpoint and by the
   * overdue-active gauge.
   */
  @Query(
      "select e from SlaBreachEventEntity e where e.resolvedAt is null order by e.breachedAt desc")
  List<SlaBreachEventEntity> findUnresolved();

  /** Lightweight count used by the Micrometer gauge. */
  @Query("select count(e) from SlaBreachEventEntity e where e.resolvedAt is null")
  long countUnresolved();

  /** Pagination-friendly list ordered newest-first for the admin screen. */
  @Query(
      "select e from SlaBreachEventEntity e "
          + "where (:onlyActive = false or e.resolvedAt is null) "
          + "order by e.breachedAt desc")
  List<SlaBreachEventEntity> findRecent(@Param("onlyActive") boolean onlyActive);
}
