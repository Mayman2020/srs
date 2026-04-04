package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.CorrespondenceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceStatusRepository extends JpaRepository<CorrespondenceStatus, Long> {

  List<CorrespondenceStatus> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  Optional<CorrespondenceStatus> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
