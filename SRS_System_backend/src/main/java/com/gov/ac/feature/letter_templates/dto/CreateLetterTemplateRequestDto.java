package com.gov.ac.feature.letter_templates.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLetterTemplateRequestDto(
    @NotBlank String code,
    @NotBlank String nameAr,
    @NotBlank String nameEn,
    String bodyHtml,
    String templateFilePath,
    @NotNull Integer sortOrder,
    @NotNull Boolean active) {}
