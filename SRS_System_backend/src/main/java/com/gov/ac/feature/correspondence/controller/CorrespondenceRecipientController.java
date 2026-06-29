package com.gov.ac.feature.correspondence.controller;

import com.gov.ac.feature.correspondence.dto.CorrespondenceRecipientDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceUserRecipientDto;
import com.gov.ac.feature.correspondence.dto.UpsertCorrespondenceRecipientRequestDto;
import com.gov.ac.feature.correspondence.dto.UpsertCorrespondenceUserRecipientRequestDto;
import com.gov.ac.feature.correspondence.service.CorrespondenceRecipientService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/correspondence")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
public class CorrespondenceRecipientController {

  private final CorrespondenceRecipientService correspondenceRecipientService;

  @GetMapping("/{correspondenceId}/recipients")
  public List<CorrespondenceRecipientDto> listRecipients(@PathVariable UUID correspondenceId) {
    return correspondenceRecipientService.list(
        correspondenceId, SecurityUtils.requireCurrentUserId());
  }

  @PostMapping("/{correspondenceId}/recipients")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public CorrespondenceRecipientDto addRecipient(
      @PathVariable UUID correspondenceId,
      @Valid @RequestBody UpsertCorrespondenceRecipientRequestDto body) {
    return correspondenceRecipientService.add(
        correspondenceId, SecurityUtils.requireCurrentUserId(), body);
  }

  @DeleteMapping("/{correspondenceId}/recipients/{recipientId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public void deleteRecipient(
      @PathVariable UUID correspondenceId, @PathVariable long recipientId) {
    correspondenceRecipientService.delete(
        correspondenceId, SecurityUtils.requireCurrentUserId(), recipientId);
  }

  @GetMapping("/{correspondenceId}/user-recipients")
  public List<CorrespondenceUserRecipientDto> listUserRecipients(
      @PathVariable UUID correspondenceId) {
    return correspondenceRecipientService.listUserRecipients(
        correspondenceId, SecurityUtils.requireCurrentUserId());
  }

  @PostMapping("/{correspondenceId}/user-recipients")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public CorrespondenceUserRecipientDto addUserRecipient(
      @PathVariable UUID correspondenceId,
      @Valid @RequestBody UpsertCorrespondenceUserRecipientRequestDto body) {
    return correspondenceRecipientService.addUserRecipient(
        correspondenceId, SecurityUtils.requireCurrentUserId(), body);
  }

  @DeleteMapping("/{correspondenceId}/user-recipients/{recipientId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public void deleteUserRecipient(
      @PathVariable UUID correspondenceId, @PathVariable long recipientId) {
    correspondenceRecipientService.deleteUserRecipient(
        correspondenceId, SecurityUtils.requireCurrentUserId(), recipientId);
  }
}
