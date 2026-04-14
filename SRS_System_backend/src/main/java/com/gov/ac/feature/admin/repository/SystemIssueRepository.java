package com.gov.ac.feature.admin.repository;

import com.gov.ac.feature.admin.entity.SystemIssueEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemIssueRepository extends JpaRepository<SystemIssueEntity, Long> {

  @EntityGraph(attributePaths = "user")
  List<SystemIssueEntity> findTop200ByOrderByCreatedAtDesc();
}
