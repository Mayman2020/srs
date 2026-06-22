package com.gov.ac.feature.correspondence.repository;

import com.gov.ac.feature.correspondence.entity.WorkflowCamundaTaskMetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowCamundaTaskMetaRepository
    extends JpaRepository<WorkflowCamundaTaskMetaEntity, String> {}
