package com.gov.ac.feature.assistant.controller;

import com.gov.ac.feature.assistant.dto.AssistantAnswerRequestDto;
import com.gov.ac.feature.assistant.dto.AssistantAnswerResponseDto;
import com.gov.ac.feature.assistant.service.AssistantService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AssistantController {

  private final AssistantService assistantService;

  @PostMapping("/answer")
  public AssistantAnswerResponseDto answer(@Valid @RequestBody AssistantAnswerRequestDto body) {
    return assistantService.answer(SecurityUtils.requireCurrentUserId(), body.query());
  }
}
