package com.gov.ac.feature.communication.controller;

import com.gov.ac.feature.communication.service.CircularService;
import com.gov.ac.feature.communication.dto.CircularInboxRowDto;
import com.gov.ac.feature.communication.dto.CreateCircularRequestDto;
import com.gov.ac.feature.communication.dto.MarkCircularReadRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/circulars")
@RequiredArgsConstructor
@Validated
public class CircularController {

  private final CircularService circularService;

  @PostMapping
  public Map<String, String> create(@Valid @RequestBody CreateCircularRequestDto body) {
    UUID id = circularService.create(body);
    return Map.of("id", id.toString());
  }

  @GetMapping("/inbox")
  public List<CircularInboxRowDto> inbox(@RequestParam @NotBlank String userId) {
    return circularService.inbox(userId);
  }

  @PostMapping("/{id}/read")
  public void markRead(
      @PathVariable UUID id, @Valid @RequestBody MarkCircularReadRequestDto body) {
    circularService.markRead(id, body.userId());
  }

  @PostMapping("/broadcast")
  public Map<String, String> broadcast(@Valid @RequestBody CreateCircularRequestDto body) {
    CreateCircularRequestDto forced =
        new CreateCircularRequestDto(
            body.title(), body.body(), body.createdBy(), true, body.recipientUserIds());
    UUID id = circularService.create(forced);
    return Map.of("id", id.toString());
  }
}
