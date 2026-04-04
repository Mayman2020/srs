package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.Priority;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriorityRepository extends JpaRepository<Priority, Long> {

  List<Priority> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  Optional<Priority> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
