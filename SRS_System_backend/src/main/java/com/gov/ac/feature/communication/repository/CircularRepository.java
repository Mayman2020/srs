package com.gov.ac.feature.communication.repository;

import com.gov.ac.feature.communication.entity.CircularEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CircularRepository extends JpaRepository<CircularEntity, UUID> {

  @Query(
      """
      SELECT DISTINCT c FROM CircularEntity c
      LEFT JOIN c.recipients r
      WHERE c.broadcast = true OR (r.id.userId = :userId)
      ORDER BY c.createdAt DESC
      """)
  List<CircularEntity> findInboxForUser(@Param("userId") String userId);
}
