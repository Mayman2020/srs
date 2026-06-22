package com.gov.ac.feature.sla.repository;

import com.gov.ac.feature.sla.entity.SlaEscalationActionTypeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaEscalationActionTypeRepository
    extends JpaRepository<SlaEscalationActionTypeEntity, String> {

  List<SlaEscalationActionTypeEntity> findByActiveTrueOrderBySortOrderAsc();
}
