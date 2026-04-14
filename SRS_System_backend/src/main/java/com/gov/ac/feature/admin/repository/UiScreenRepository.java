package com.gov.ac.feature.admin.repository;

import com.gov.ac.feature.admin.entity.UiScreenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UiScreenRepository extends JpaRepository<UiScreenEntity, Long> {

  List<UiScreenEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  List<UiScreenEntity> findByDeletedAtIsNullAndShowInShellNavTrueOrderBySortOrderAsc();

  Optional<UiScreenEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);
}
