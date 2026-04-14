package com.gov.ac.feature.letter_templates.mapper;

import com.gov.ac.feature.attachment.service.AttachmentStorageProperties;
import com.gov.ac.feature.letter_templates.entity.CorrespondenceLetterTemplateEntity;
import com.gov.ac.feature.letter_templates.dto.LetterTemplateDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class LetterTemplateMapper {

  private final AttachmentStorageProperties storageProperties;

  public LetterTemplateDto toDto(CorrespondenceLetterTemplateEntity template) {
    String body = template.getBodyHtml() != null ? template.getBodyHtml() : "";
    String path = template.getTemplateFilePath();
    if (StringUtils.hasText(path)) {
      try {
        Path root = Paths.get(storageProperties.root()).toAbsolutePath().normalize();
        Path file = root.resolve(path.trim()).normalize();
        if (file.startsWith(root) && Files.isRegularFile(file)) {
          body = Files.readString(file, StandardCharsets.UTF_8);
        } else {
          log.warn(
              "Letter template file missing or outside root: code={} path={}",
              template.getCode(),
              path);
        }
      } catch (IOException e) {
        log.warn(
            "Could not read letter template file code={} path={}: {}",
            template.getCode(),
            path,
            e.getMessage());
      }
    }
    return new LetterTemplateDto(
        template.getCode(),
        template.getNameAr(),
        template.getNameEn(),
        body,
        template.getSortOrder() != null ? template.getSortOrder() : 0,
        path);
  }
}
