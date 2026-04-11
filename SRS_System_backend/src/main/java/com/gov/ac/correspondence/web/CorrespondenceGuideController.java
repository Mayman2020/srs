package com.gov.ac.correspondence.web;

import com.gov.ac.correspondence.dto.CorrespondenceLinkListItemDto;
import com.gov.ac.correspondence.dto.CorrespondenceNonarchivedItemDto;
import com.gov.ac.correspondence.dto.CreateCorrespondenceLinkRequest;
import com.gov.ac.correspondence.dto.UpsertCorrespondenceNonarchivedItemRequest;
import com.gov.ac.correspondence.service.CorrespondenceGuideService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/correspondence")
@RequiredArgsConstructor
public class CorrespondenceGuideController {

  private final CorrespondenceGuideService correspondenceGuideService;

  @GetMapping("/{correspondenceId}/links")
  public List<CorrespondenceLinkListItemDto> listLinks(@PathVariable UUID correspondenceId) {
    return correspondenceGuideService.listLinks(correspondenceId, SecurityUtils.requireCurrentUserId());
  }

  @PostMapping("/{correspondenceId}/links")
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceLinkListItemDto addLink(
      @PathVariable UUID correspondenceId, @Valid @RequestBody CreateCorrespondenceLinkRequest body) {
    return correspondenceGuideService.addLink(
        correspondenceId, SecurityUtils.requireCurrentUserId(), body);
  }

  @DeleteMapping("/{correspondenceId}/links/{linkId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteLink(@PathVariable UUID correspondenceId, @PathVariable long linkId) {
    correspondenceGuideService.deleteLink(correspondenceId, SecurityUtils.requireCurrentUserId(), linkId);
  }

  @GetMapping("/{correspondenceId}/nonarchived-items")
  public List<CorrespondenceNonarchivedItemDto> listNonarchived(@PathVariable UUID correspondenceId) {
    return correspondenceGuideService.listNonarchived(
        correspondenceId, SecurityUtils.requireCurrentUserId());
  }

  @PostMapping("/{correspondenceId}/nonarchived-items")
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceNonarchivedItemDto addNonarchived(
      @PathVariable UUID correspondenceId,
      @Valid @RequestBody UpsertCorrespondenceNonarchivedItemRequest body) {
    return correspondenceGuideService.addNonarchived(
        correspondenceId, SecurityUtils.requireCurrentUserId(), body);
  }

  @PutMapping("/{correspondenceId}/nonarchived-items/{itemId}")
  public CorrespondenceNonarchivedItemDto updateNonarchived(
      @PathVariable UUID correspondenceId,
      @PathVariable long itemId,
      @Valid @RequestBody UpsertCorrespondenceNonarchivedItemRequest body) {
    return correspondenceGuideService.updateNonarchived(
        correspondenceId, SecurityUtils.requireCurrentUserId(), itemId, body);
  }

  @DeleteMapping("/{correspondenceId}/nonarchived-items/{itemId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNonarchived(
      @PathVariable UUID correspondenceId, @PathVariable long itemId) {
    correspondenceGuideService.deleteNonarchived(
        correspondenceId, SecurityUtils.requireCurrentUserId(), itemId);
  }
}
