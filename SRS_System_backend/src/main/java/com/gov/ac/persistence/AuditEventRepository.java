package com.gov.ac.persistence;

import com.gov.ac.domain.audit.AuditEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  @Query(
      """
      SELECT e FROM AuditEvent e
      WHERE (:actor IS NULL OR e.actorUserId = :actor)
        AND (:action IS NULL OR e.actionCode = :action)
        AND e.occurredAt >= :from
        AND e.occurredAt <= :to
      ORDER BY e.occurredAt DESC
      """)
  List<AuditEvent> search(
      @Param("actor") String actor,
      @Param("action") String action,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
