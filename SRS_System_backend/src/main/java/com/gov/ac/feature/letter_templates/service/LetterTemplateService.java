package com.gov.ac.feature.letter_templates.service;

import com.gov.ac.domain.correspondence.CorrespondenceLetterTemplate;
import com.gov.ac.feature.letter_templates.dto.LetterTemplateDto;
import com.gov.ac.persistence.CorrespondenceLetterTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LetterTemplateService {

  private final CorrespondenceLetterTemplateRepository repository;

  @Transactional(readOnly = true)
  public List<LetterTemplateDto> listActive() {
    return repository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(LetterTemplateService::toDto)
        .toList();
  }

  private static LetterTemplateDto toDto(CorrespondenceLetterTemplate t) {
    return new LetterTemplateDto(
        t.getCode(),
        t.getNameAr(),
        t.getNameEn(),
        t.getBodyHtml() != null ? t.getBodyHtml() : "",
        t.getSortOrder() != null ? t.getSortOrder() : 0);
  }
}
