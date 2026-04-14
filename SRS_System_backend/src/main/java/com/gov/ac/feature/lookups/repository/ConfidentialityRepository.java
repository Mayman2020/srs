package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfidentialityRepository extends JpaRepository<ConfidentialityEntity, Long> {

  List<ConfidentialityEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<ConfidentialityEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<ConfidentialityEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  Optional<ConfidentialityEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);
}
