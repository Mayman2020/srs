package com.gov.ac.feature.correspondence.controller;

import com.gov.ac.feature.correspondence.dto.CorrespondenceAttachmentDetailDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceAttachmentFormDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCancelRequestDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreateFormDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreatedResponseDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceDetailResponseDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceDraftSaveRequestDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceListItemDto;
import com.gov.ac.feature.correspondence.dto.CorrespondencePatchRequestDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceReplySendRequestDto;
import com.gov.ac.feature.correspondence.dto.WorkflowActionRequestDto;
import com.gov.ac.feature.correspondence.dto.WorkflowDelegateRequestDto;
import com.gov.ac.feature.correspondence.service.CorrespondenceAttachmentMutationService;
import com.gov.ac.feature.correspondence.service.CorrespondenceCancelService;
import com.gov.ac.feature.correspondence.service.CorrespondenceCommentService;
import com.gov.ac.feature.correspondence.service.CorrespondenceCreateService;
import com.gov.ac.feature.correspondence.service.CorrespondenceDetailService;
import com.gov.ac.feature.correspondence.service.CorrespondenceDraftReplyService;
import com.gov.ac.feature.correspondence.service.CorrespondenceListService;
import com.gov.ac.feature.correspondence.service.CorrespondenceUpdateService;
import com.gov.ac.feature.correspondence.dto.CreateCorrespondenceCommentRequestDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCommentDetailDto;
import com.gov.ac.feature.correspondence.dto.LegalHoldCorrespondenceRequestDto;
import com.gov.ac.feature.correspondence.dto.WorkflowActionAvailableDto;
import com.gov.ac.feature.correspondence.service.CorrespondenceWorkflowActionService;
import com.gov.ac.feature.correspondence.service.CorrespondenceWorkflowAvailabilityService;
import com.gov.ac.feature.retention.dto.LegalHoldDto;
import com.gov.ac.feature.retention.dto.LegalHoldPlaceRequestDto;
import com.gov.ac.feature.retention.dto.LegalHoldReleaseRequestDto;
import com.gov.ac.feature.retention.service.RetentionAdminService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/correspondence")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
public class CorrespondenceController {

  private final CorrespondenceCreateService correspondenceCreateService;
  private final CorrespondenceDetailService correspondenceDetailService;
  private final CorrespondenceListService correspondenceListService;
  private final CorrespondenceWorkflowActionService correspondenceWorkflowActionService;
  private final CorrespondenceWorkflowAvailabilityService correspondenceWorkflowAvailabilityService;
  private final RetentionAdminService retentionAdminService;
  private final CorrespondenceCommentService correspondenceCommentService;
  private final CorrespondenceCancelService correspondenceCancelService;
  private final CorrespondenceAttachmentMutationService correspondenceAttachmentMutationService;
  private final CorrespondenceDraftReplyService correspondenceDraftReplyService;
  private final CorrespondenceUpdateService correspondenceUpdateService;

  @GetMapping
  public Page<CorrespondenceListItemDto> list(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) Instant createdFrom,
      @RequestParam(required = false) Instant createdTo,
      @RequestParam(required = false, name = "q") String freeText) {
    return correspondenceListService.search(
        pageable,
        status,
        type,
        priority,
        createdFrom,
        createdTo,
        freeText,
        SecurityUtils.requireCurrentUserId());
  }

  @GetMapping("/by-barcode/{barcode}")
  public CorrespondenceDetailResponseDto getByBarcode(@PathVariable String barcode) {
    return correspondenceDetailService.getByBarcode(barcode, SecurityUtils.requireCurrentUserId());
  }

  @GetMapping("/{id}/workflow-actions")
  @PreAuthorize("@effectivePermission.has('WORKFLOW_TASK_ACTION')")
  public List<WorkflowActionAvailableDto> workflowActions(@PathVariable UUID id) {
    return correspondenceWorkflowAvailabilityService.listAvailableWorkflowActions(
        id, SecurityUtils.requireCurrentUserId());
  }

  @GetMapping("/{id}/legal-holds/active")
  @PreAuthorize("@effectivePermission.has('LEGAL_HOLD_VIEW')")
  public List<LegalHoldDto> activeLegalHolds(@PathVariable UUID id) {
    return retentionAdminService.listActiveHoldsForCorrespondence(id);
  }

  @PostMapping("/{id}/legal-holds")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('LEGAL_HOLD_MANAGE')")
  public LegalHoldDto placeLegalHold(
      @PathVariable UUID id, @Valid @RequestBody LegalHoldCorrespondenceRequestDto body) {
    return retentionAdminService.placeHold(
        SecurityUtils.requireCurrentUserId(),
        new LegalHoldPlaceRequestDto(id, body.reason()));
  }

  @PostMapping("/{id}/legal-holds/{holdId}/release")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('LEGAL_HOLD_MANAGE')")
  public void releaseLegalHold(
      @PathVariable UUID id,
      @PathVariable UUID holdId,
      @Valid @RequestBody LegalHoldReleaseRequestDto body) {
    retentionAdminService.releaseHold(SecurityUtils.requireCurrentUserId(), holdId, body);
  }

  @GetMapping("/{id}")
  public CorrespondenceDetailResponseDto getById(@PathVariable UUID id) {
    return correspondenceDetailService.getById(id, SecurityUtils.requireCurrentUserId());
  }

  @PatchMapping("/{id}")
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public CorrespondenceDetailResponseDto patch(
      @PathVariable UUID id, @Valid @RequestBody CorrespondencePatchRequestDto body) {
    return correspondenceUpdateService.patch(id, SecurityUtils.requireCurrentUserId(), body);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_CREATE')")
  public CorrespondenceCreatedResponseDto create(@Valid @RequestBody CorrespondenceCreateFormDto form) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    return correspondenceCreateService.create(userId, form);
  }

  /** Complete the current user's active Camunda task for this correspondence (same auth as view). */
  @PostMapping("/{id}/actions")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('WORKFLOW_TASK_ACTION')")
  public void workflowAction(
      @PathVariable UUID id, @Valid @RequestBody(required = false) WorkflowActionRequestDto body) {
    String action = body != null ? body.action() : null;
    String comment = body != null ? body.comment() : null;
    correspondenceWorkflowActionService.completeActiveAssigneeTask(
        id,
        SecurityUtils.requireCurrentUserId(),
        action,
        comment,
        body != null ? body.targetUserId() : null,
        body != null ? body.targetDepartmentId() : null);
  }

  @PostMapping("/{id}/workflow-delegate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('WORKFLOW_TASK_ACTION')")
  public void workflowDelegate(
      @PathVariable UUID id, @Valid @RequestBody WorkflowDelegateRequestDto body) {
    correspondenceWorkflowActionService.delegateActiveAssigneeTask(
        id, SecurityUtils.requireCurrentUserId(), body.delegateeUserId());
  }

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceCommentDetailDto addComment(
      @PathVariable UUID id, @Valid @RequestBody CreateCorrespondenceCommentRequestDto request) {
    return correspondenceCommentService.addComment(
        id, SecurityUtils.requireCurrentUserId(), request);
  }

  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_DELETE')")
  public void cancel(
      @PathVariable UUID id,
      @RequestBody(required = false) @Valid CorrespondenceCancelRequestDto body) {
    correspondenceCancelService.cancel(
        id, SecurityUtils.requireCurrentUserId(), body != null ? body : new CorrespondenceCancelRequestDto(null));
  }

  @PostMapping("/{id}/attachments")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public CorrespondenceAttachmentDetailDto addAttachment(
      @PathVariable UUID id, @Valid @RequestBody CorrespondenceAttachmentFormDto form) {
    return correspondenceAttachmentMutationService.addAttachment(
        id, SecurityUtils.requireCurrentUserId(), form);
  }

  @PostMapping("/{id}/draft")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveDraft(
      @PathVariable UUID id,
      @RequestBody(required = false) @Valid CorrespondenceDraftSaveRequestDto body) {
    correspondenceDraftReplyService.saveDraft(
        id,
        SecurityUtils.requireCurrentUserId(),
        body != null ? body : new CorrespondenceDraftSaveRequestDto(""));
  }

  @PostMapping("/{id}/reply")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void sendReply(
      @PathVariable UUID id, @Valid @RequestBody CorrespondenceReplySendRequestDto body) {
    correspondenceDraftReplyService.sendReply(id, SecurityUtils.requireCurrentUserId(), body);
  }
}
