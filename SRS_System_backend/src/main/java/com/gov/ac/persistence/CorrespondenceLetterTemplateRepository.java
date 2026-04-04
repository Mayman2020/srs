package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.CorrespondenceLetterTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceLetterTemplateRepository
    extends JpaRepository<CorrespondenceLetterTemplate, Long> {

  List<CorrespondenceLetterTemplate> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();
}
