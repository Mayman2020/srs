package com.gov.ac.feature.sla.repository;

import com.gov.ac.feature.sla.entity.SlaEscalationStepEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaEscalationStepRepository
    extends JpaRepository<SlaEscalationStepEntity, Long> {

  List<SlaEscalationStepEntity>
      findByPolicy_IdAndActiveTrueAndDeletedAtIsNullOrderByStepOrderAsc(Long policyId);

  List<SlaEscalationStepEntity> findByPolicy_IdAndDeletedAtIsNullOrderByStepOrderAsc(
      Long policyId);
}
