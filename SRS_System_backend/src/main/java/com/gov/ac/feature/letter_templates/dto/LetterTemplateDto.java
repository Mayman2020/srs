package com.gov.ac.feature.letter_templates.dto;

public record LetterTemplateDto(
    String code, String nameAr, String nameEn, String bodyHtml, int sortOrder) {}
