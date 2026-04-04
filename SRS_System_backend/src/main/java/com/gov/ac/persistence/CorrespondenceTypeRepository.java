package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.CorrespondenceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceTypeRepository extends JpaRepository<CorrespondenceType, Long> {

  List<CorrespondenceType> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  Optional<CorrespondenceType> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
