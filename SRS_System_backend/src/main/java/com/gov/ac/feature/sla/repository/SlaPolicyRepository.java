package com.gov.ac.feature.sla.repository;

import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicyEntity, Long> {

  Optional<SlaPolicyEntity> findByIdAndDeletedAtIsNull(Long id);

  Optional<SlaPolicyEntity> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  List<SlaPolicyEntity> findByActiveTrueAndDeletedAtIsNull();

  List<SlaPolicyEntity> findByDeletedAtIsNullOrderByIdAsc();
}
