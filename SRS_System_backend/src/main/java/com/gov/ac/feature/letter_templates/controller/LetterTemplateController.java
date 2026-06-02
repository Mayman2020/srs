package com.gov.ac.feature.letter_templates.controller;

import com.gov.ac.feature.letter_templates.dto.LetterTemplateDto;
import com.gov.ac.feature.letter_templates.service.LetterTemplateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/letter-templates")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LetterTemplateController {

  private final LetterTemplateService letterTemplateService;

  @GetMapping
  public List<LetterTemplateDto> list() {
    return letterTemplateService.listActive();
  }
}
