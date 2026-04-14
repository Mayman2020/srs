package com.gov.ac.feature.audit.repository;

import com.gov.ac.feature.audit.entity.AuditEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

  @Query(
      """
      SELECT e FROM AuditEventEntity e
      WHERE (:actor IS NULL OR e.actorUserId = :actor)
        AND (:action IS NULL OR e.actionCode = :action)
        AND e.occurredAt >= :from
        AND e.occurredAt <= :to
      ORDER BY e.occurredAt DESC
      """)
  List<AuditEventEntity> search(
      @Param("actor") String actor,
      @Param("action") String action,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
