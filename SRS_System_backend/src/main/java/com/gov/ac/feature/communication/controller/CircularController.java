package com.gov.ac.feature.communication.controller;

import com.gov.ac.feature.communication.dto.CircularInboxRowDto;
import com.gov.ac.feature.communication.dto.CreateCircularRequestDto;
import com.gov.ac.feature.communication.service.CircularService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Circular create / broadcast / inbox / mark-read endpoints.
 *
 * <p>{@code userId} is always derived from the JWT principal (see
 * {@link SecurityUtils#requireCurrentUserId()}) — never from a query string or request body — to
 * close the IDOR present in the original implementation.
 */
@RestController
@RequestMapping("/api/v1/circulars")
@RequiredArgsConstructor
@Validated
public class CircularController {

  private final CircularService circularService;

  @PostMapping
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_CREATE')")
  public Map<String, String> create(@Valid @RequestBody CreateCircularRequestDto body) {
    UUID id = circularService.create(stamp(body, false));
    return Map.of("id", id.toString());
  }

  /** Authenticated user only ever sees their own inbox. */
  @GetMapping("/inbox")
  @PreAuthorize("isAuthenticated()")
  public List<CircularInboxRowDto> inbox() {
    return circularService.inbox(SecurityUtils.requireCurrentUserId().toString());
  }

  @PostMapping("/{id}/read")
  @PreAuthorize("isAuthenticated()")
  public void markRead(@PathVariable UUID id) {
    circularService.markRead(id, SecurityUtils.requireCurrentUserId().toString());
  }

  @PostMapping("/broadcast")
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_CREATE') and "
      + "@effectivePermission.has('NOTIFICATION_DISPATCH')")
  public Map<String, String> broadcast(@Valid @RequestBody CreateCircularRequestDto body) {
    UUID id = circularService.create(stamp(body, true));
    return Map.of("id", id.toString());
  }

  /** Always force {@code createdBy} to the JWT user and broadcast to the requested mode. */
  private CreateCircularRequestDto stamp(CreateCircularRequestDto body, boolean forceBroadcast) {
    String me = SecurityUtils.requireCurrentUserId().toString();
    boolean broadcast = forceBroadcast || body.broadcast();
    return new CreateCircularRequestDto(
        body.title(), body.body(), me, broadcast, body.recipientUserIds());
  }
}
