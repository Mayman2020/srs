package com.gov.ac.feature.correspondence.outbound.controller;

import com.gov.ac.feature.correspondence.outbound.dto.OutboundDeliveryDto;
import com.gov.ac.feature.correspondence.outbound.dto.UpsertOutboundDeliveryRequestDto;
import com.gov.ac.feature.correspondence.outbound.service.OutboundDeliveryService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbound-deliveries")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OutboundDeliveryController {

  private final OutboundDeliveryService outboundDeliveryService;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
  public List<OutboundDeliveryDto> list(
      @RequestParam(name = "correspondenceId", required = false) UUID correspondenceId) {
    return outboundDeliveryService.list(correspondenceId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public OutboundDeliveryDto create(@Valid @RequestBody UpsertOutboundDeliveryRequestDto body) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    return outboundDeliveryService.create(actor, body);
  }

  @PutMapping("/{id}")
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public OutboundDeliveryDto update(
      @PathVariable long id, @Valid @RequestBody UpsertOutboundDeliveryRequestDto body) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    return outboundDeliveryService.update(actor, id, body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public void delete(@PathVariable long id) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    outboundDeliveryService.delete(actor, id);
  }
}
