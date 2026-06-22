package com.gov.ac.feature.letter_templates.controller;

import com.gov.ac.feature.letter_templates.dto.CreateLetterTemplateRequestDto;
import com.gov.ac.feature.letter_templates.dto.LetterTemplateAdminDto;
import com.gov.ac.feature.letter_templates.dto.LetterTemplateDto;
import com.gov.ac.feature.letter_templates.dto.UpdateLetterTemplateRequestDto;
import com.gov.ac.feature.letter_templates.service.LetterTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

  @GetMapping("/admin")
  @PreAuthorize("@effectivePermission.has('LETTER_TEMPLATE_MANAGE')")
  public List<LetterTemplateAdminDto> listAdmin() {
    return letterTemplateService.listAllAdmin();
  }

  @PostMapping("/admin")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('LETTER_TEMPLATE_MANAGE')")
  public LetterTemplateAdminDto create(@Valid @RequestBody CreateLetterTemplateRequestDto body) {
    return letterTemplateService.create(body);
  }

  @PutMapping("/admin/{id}")
  @PreAuthorize("@effectivePermission.has('LETTER_TEMPLATE_MANAGE')")
  public LetterTemplateAdminDto update(
      @PathVariable Long id, @Valid @RequestBody UpdateLetterTemplateRequestDto body) {
    return letterTemplateService.update(id, body);
  }

  @DeleteMapping("/admin/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('LETTER_TEMPLATE_MANAGE')")
  public void delete(@PathVariable Long id) {
    letterTemplateService.delete(id);
  }
}
