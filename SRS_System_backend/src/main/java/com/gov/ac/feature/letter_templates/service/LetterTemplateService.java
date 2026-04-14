package com.gov.ac.feature.letter_templates.service;

import com.gov.ac.feature.letter_templates.dto.LetterTemplateDto;
import com.gov.ac.feature.letter_templates.mapper.LetterTemplateMapper;
import com.gov.ac.feature.letter_templates.repository.CorrespondenceLetterTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LetterTemplateService {

  private final CorrespondenceLetterTemplateRepository repository;
  private final LetterTemplateMapper letterTemplateMapper;

  @Transactional(readOnly = true)
  public List<LetterTemplateDto> listActive() {
    return repository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(letterTemplateMapper::toDto)
        .toList();
  }
}
