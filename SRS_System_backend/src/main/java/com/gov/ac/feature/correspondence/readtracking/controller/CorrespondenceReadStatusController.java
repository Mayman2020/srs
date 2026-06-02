package com.gov.ac.feature.correspondence.readtracking.controller;

import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceAckRequestDto;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadReceiptDto;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadStatusSummaryDto;
import com.gov.ac.feature.correspondence.readtracking.service.CorrespondenceReadTrackingService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slice 1 controller: self-service acknowledgement (any authenticated viewer of the
 * correspondence) and cross-user read-status listing (restricted to {@code
 * CORRESPONDENCE_READ_STATUS_VIEW}).
 */
@RestController
@RequestMapping("/api/v1/correspondence")
@RequiredArgsConstructor
public class CorrespondenceReadStatusController {

  private final CorrespondenceReadTrackingService readTrackingService;

  @PostMapping("/{id}/ack")
  @PreAuthorize("isAuthenticated()")
  public CorrespondenceReadReceiptDto acknowledge(
      @PathVariable("id") UUID id,
      @RequestBody(required = false) @Valid CorrespondenceAckRequestDto body) {
    String comment = body != null ? body.comment() : null;
    return readTrackingService.acknowledge(id, SecurityUtils.requireCurrentUserId(), comment);
  }

  @GetMapping("/{id}/read-status")
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_READ_STATUS_VIEW')")
  public CorrespondenceReadStatusSummaryDto readStatus(@PathVariable("id") UUID id) {
    return readTrackingService.listForCorrespondence(id);
  }
}
