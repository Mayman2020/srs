package com.gov.ac.modules.communication.web;

import com.gov.ac.modules.communication.service.CircularService;
import com.gov.ac.modules.communication.web.dto.CircularInboxRow;
import com.gov.ac.modules.communication.web.dto.CreateCircularRequest;
import com.gov.ac.modules.communication.web.dto.MarkCircularReadRequest;
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
  public Map<String, String> create(@Valid @RequestBody CreateCircularRequest body) {
    UUID id = circularService.create(body);
    return Map.of("id", id.toString());
  }

  @GetMapping("/inbox")
  public List<CircularInboxRow> inbox(@RequestParam @NotBlank String userId) {
    return circularService.inbox(userId);
  }

  @PostMapping("/{id}/read")
  public void markRead(
      @PathVariable UUID id, @Valid @RequestBody MarkCircularReadRequest body) {
    circularService.markRead(id, body.userId());
  }

  @PostMapping("/broadcast")
  public Map<String, String> broadcast(@Valid @RequestBody CreateCircularRequest body) {
    CreateCircularRequest forced =
        new CreateCircularRequest(
            body.title(), body.body(), body.createdBy(), true, body.recipientUserIds());
    UUID id = circularService.create(forced);
    return Map.of("id", id.toString());
  }
}
