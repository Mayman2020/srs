package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.PriorityEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriorityRepository extends JpaRepository<PriorityEntity, Long> {

  List<PriorityEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<PriorityEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<PriorityEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  Optional<PriorityEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);
}
