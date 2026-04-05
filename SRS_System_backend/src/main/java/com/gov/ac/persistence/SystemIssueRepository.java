package com.gov.ac.persistence;

import com.gov.ac.domain.admin.SystemIssue;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemIssueRepository extends JpaRepository<SystemIssue, Long> {

  @EntityGraph(attributePaths = "user")
  List<SystemIssue> findTop200ByOrderByCreatedAtDesc();
}
