package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.WorkflowInstanceStatusEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceStatusRepository extends JpaRepository<WorkflowInstanceStatusEntity, Long> {

  Optional<WorkflowInstanceStatusEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
