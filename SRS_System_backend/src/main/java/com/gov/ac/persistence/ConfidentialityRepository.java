package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.Confidentiality;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfidentialityRepository extends JpaRepository<Confidentiality, Long> {

  List<Confidentiality> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  Optional<Confidentiality> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
