package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.WorkflowInstanceStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceStatusRepository extends JpaRepository<WorkflowInstanceStatus, Long> {

  Optional<WorkflowInstanceStatus> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
