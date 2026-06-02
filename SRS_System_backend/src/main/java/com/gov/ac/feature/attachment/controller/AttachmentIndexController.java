package com.gov.ac.feature.attachment.controller;

import com.gov.ac.feature.correspondence.dto.AttachmentIndexEntryDto;
import com.gov.ac.feature.correspondence.dto.UpsertAttachmentIndexEntryRequestDto;
import com.gov.ac.feature.correspondence.service.CorrespondenceGuideService;
import com.gov.ac.security.SecurityUtils;
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
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
public class AttachmentIndexController {

  private final CorrespondenceGuideService correspondenceGuideService;

  @GetMapping("/{attachmentId}/index-entries")
  public List<AttachmentIndexEntryDto> list(@PathVariable Long attachmentId) {
    return correspondenceGuideService.listIndexEntries(attachmentId, SecurityUtils.requireCurrentUserId());
  }

  @PostMapping("/{attachmentId}/index-entries")
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentIndexEntryDto add(
      @PathVariable Long attachmentId, @Valid @RequestBody UpsertAttachmentIndexEntryRequestDto body) {
    return correspondenceGuideService.addIndexEntry(
        attachmentId, SecurityUtils.requireCurrentUserId(), body);
  }

  @PutMapping("/{attachmentId}/index-entries/{entryId}")
  public AttachmentIndexEntryDto update(
      @PathVariable Long attachmentId,
      @PathVariable long entryId,
      @Valid @RequestBody UpsertAttachmentIndexEntryRequestDto body) {
    return correspondenceGuideService.updateIndexEntry(
        attachmentId, SecurityUtils.requireCurrentUserId(), entryId, body);
  }

  @DeleteMapping("/{attachmentId}/index-entries/{entryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long attachmentId, @PathVariable long entryId) {
    correspondenceGuideService.deleteIndexEntry(
        attachmentId, SecurityUtils.requireCurrentUserId(), entryId);
  }
}
