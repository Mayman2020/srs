package com.gov.ac.feature.letter_templates.repository;

import com.gov.ac.feature.letter_templates.entity.CorrespondenceLetterTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceLetterTemplateRepository
    extends JpaRepository<CorrespondenceLetterTemplateEntity, Long> {

  List<CorrespondenceLetterTemplateEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<CorrespondenceLetterTemplateEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<CorrespondenceLetterTemplateEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeAndDeletedAtIsNull(String code);
}
