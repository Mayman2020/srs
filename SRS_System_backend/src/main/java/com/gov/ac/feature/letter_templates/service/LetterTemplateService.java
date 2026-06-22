package com.gov.ac.feature.letter_templates.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.letter_templates.dto.CreateLetterTemplateRequestDto;
import com.gov.ac.feature.letter_templates.dto.LetterTemplateAdminDto;
import com.gov.ac.feature.letter_templates.dto.LetterTemplateDto;
import com.gov.ac.feature.letter_templates.dto.UpdateLetterTemplateRequestDto;
import com.gov.ac.feature.letter_templates.entity.CorrespondenceLetterTemplateEntity;
import com.gov.ac.feature.letter_templates.mapper.LetterTemplateMapper;
import com.gov.ac.feature.letter_templates.repository.CorrespondenceLetterTemplateRepository;
import java.time.Instant;
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

  @Transactional(readOnly = true)
  public List<LetterTemplateAdminDto> listAllAdmin() {
    return repository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(this::toAdminDto)
        .toList();
  }

  @Transactional
  public LetterTemplateAdminDto create(CreateLetterTemplateRequestDto req) {
    String code = req.code().trim();
    if (repository.existsByCodeAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Template code already exists");
    }
    CorrespondenceLetterTemplateEntity row = new CorrespondenceLetterTemplateEntity();
    row.setCode(code);
    applyUpsert(row, req.nameAr(), req.nameEn(), req.bodyHtml(), req.templateFilePath(), req.sortOrder(), req.active());
    return toAdminDto(repository.save(row));
  }

  @Transactional
  public LetterTemplateAdminDto update(Long id, UpdateLetterTemplateRequestDto req) {
    CorrespondenceLetterTemplateEntity row = load(id);
    applyUpsert(row, req.nameAr(), req.nameEn(), req.bodyHtml(), req.templateFilePath(), req.sortOrder(), req.active());
    return toAdminDto(row);
  }

  @Transactional
  public void delete(Long id) {
    CorrespondenceLetterTemplateEntity row = load(id);
    row.setDeletedAt(Instant.now());
  }

  private CorrespondenceLetterTemplateEntity load(Long id) {
    return repository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException("Letter template not found"));
  }

  private static void applyUpsert(
      CorrespondenceLetterTemplateEntity row,
      String nameAr,
      String nameEn,
      String bodyHtml,
      String templateFilePath,
      Integer sortOrder,
      Boolean active) {
    row.setNameAr(nameAr.trim());
    row.setNameEn(nameEn.trim());
    row.setBodyHtml(bodyHtml != null ? bodyHtml : "");
    row.setTemplateFilePath(templateFilePath != null && !templateFilePath.isBlank() ? templateFilePath.trim() : null);
    row.setSortOrder(sortOrder != null ? sortOrder : 0);
    row.setActive(Boolean.TRUE.equals(active));
  }

  private LetterTemplateAdminDto toAdminDto(CorrespondenceLetterTemplateEntity template) {
    LetterTemplateDto dto = letterTemplateMapper.toDto(template);
    return new LetterTemplateAdminDto(
        template.getId(),
        dto.code(),
        dto.nameAr(),
        dto.nameEn(),
        dto.bodyHtml(),
        dto.sortOrder(),
        Boolean.TRUE.equals(template.getActive()),
        dto.templateFilePath());
  }
}
