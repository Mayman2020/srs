package com.gov.ac.feature.leave.repository;

import com.gov.ac.feature.leave.entity.LeaveStatusEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveStatusRepository extends JpaRepository<LeaveStatusEntity, Long> {

  List<LeaveStatusEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  Optional<LeaveStatusEntity> findByInitialTrueAndActiveTrueAndDeletedAtIsNull();

  Optional<LeaveStatusEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  Optional<LeaveStatusEntity> findByIdAndDeletedAtIsNull(Long id);

  List<LeaveStatusEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);
}
