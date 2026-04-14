package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceTypeRepository extends JpaRepository<CorrespondenceTypeEntity, Long> {

  List<CorrespondenceTypeEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<CorrespondenceTypeEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<CorrespondenceTypeEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);

  Optional<CorrespondenceTypeEntity> findByIdAndDeletedAtIsNull(Long id);
}
