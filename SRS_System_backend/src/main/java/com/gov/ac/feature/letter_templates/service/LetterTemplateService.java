package com.gov.ac.feature.letter_templates.service;

import com.gov.ac.attachment.AttachmentStorageProperties;
import com.gov.ac.domain.correspondence.CorrespondenceLetterTemplate;
import com.gov.ac.feature.letter_templates.dto.LetterTemplateDto;
import com.gov.ac.persistence.CorrespondenceLetterTemplateRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class LetterTemplateService {

  private final CorrespondenceLetterTemplateRepository repository;
  private final AttachmentStorageProperties storageProperties;

  @Transactional(readOnly = true)
  public List<LetterTemplateDto> listActive() {
    return repository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(this::toDto)
        .toList();
  }

  private LetterTemplateDto toDto(CorrespondenceLetterTemplate t) {
    String body = t.getBodyHtml() != null ? t.getBodyHtml() : "";
    String path = t.getTemplateFilePath();
    if (StringUtils.hasText(path)) {
      try {
        Path root = Paths.get(storageProperties.root()).toAbsolutePath().normalize();
        Path file = root.resolve(path.trim()).normalize();
        if (file.startsWith(root) && Files.isRegularFile(file)) {
          body = Files.readString(file, StandardCharsets.UTF_8);
        } else {
          log.warn("Letter template file missing or outside root: code={} path={}", t.getCode(), path);
        }
      } catch (IOException e) {
        log.warn("Could not read letter template file code={} path={}: {}", t.getCode(), path, e.getMessage());
      }
    }
    return new LetterTemplateDto(
        t.getCode(),
        t.getNameAr(),
        t.getNameEn(),
        body,
        t.getSortOrder() != null ? t.getSortOrder() : 0,
        path);
  }
}
