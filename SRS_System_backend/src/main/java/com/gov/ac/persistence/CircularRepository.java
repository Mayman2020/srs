package com.gov.ac.persistence;

import com.gov.ac.domain.communication.Circular;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CircularRepository extends JpaRepository<Circular, UUID> {

  @Query(
      """
      SELECT DISTINCT c FROM Circular c
      LEFT JOIN c.recipients r
      WHERE c.broadcast = true OR (r.id.userId = :userId)
      ORDER BY c.createdAt DESC
      """)
  List<Circular> findInboxForUser(@Param("userId") String userId);
}
