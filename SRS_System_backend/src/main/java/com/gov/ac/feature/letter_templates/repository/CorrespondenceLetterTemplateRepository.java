package com.gov.ac.feature.letter_templates.repository;

import com.gov.ac.feature.letter_templates.entity.CorrespondenceLetterTemplateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceLetterTemplateRepository
    extends JpaRepository<CorrespondenceLetterTemplateEntity, Long> {

  List<CorrespondenceLetterTemplateEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();
}
