package com.gov.ac.feature.letter_templates.dto;

public record LetterTemplateAdminDto(
    Long id,
    String code,
    String nameAr,
    String nameEn,
    String bodyHtml,
    int sortOrder,
    boolean active,
    String templateFilePath) {}
