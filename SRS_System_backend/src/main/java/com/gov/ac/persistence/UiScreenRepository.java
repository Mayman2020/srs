package com.gov.ac.persistence;

import com.gov.ac.domain.admin.UiScreen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UiScreenRepository extends JpaRepository<UiScreen, Long> {

  List<UiScreen> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<UiScreen> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);
}
