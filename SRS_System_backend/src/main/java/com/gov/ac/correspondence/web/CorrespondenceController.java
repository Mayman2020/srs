package com.gov.ac.correspondence.web;

import com.gov.ac.correspondence.dto.CorrespondenceAttachmentDetailDto;
import com.gov.ac.correspondence.dto.CorrespondenceAttachmentForm;
import com.gov.ac.correspondence.dto.CorrespondenceCancelRequest;
import com.gov.ac.correspondence.dto.CorrespondenceCreateForm;
import com.gov.ac.correspondence.dto.CorrespondenceCreatedResponse;
import com.gov.ac.correspondence.dto.CorrespondenceDetailResponse;
import com.gov.ac.correspondence.dto.CorrespondenceDraftSaveRequest;
import com.gov.ac.correspondence.dto.CorrespondenceListItemDto;
import com.gov.ac.correspondence.dto.CorrespondenceReplySendRequest;
import com.gov.ac.correspondence.dto.WorkflowActionRequest;
import com.gov.ac.correspondence.dto.WorkflowDelegateRequest;
import com.gov.ac.correspondence.service.CorrespondenceAttachmentMutationService;
import com.gov.ac.correspondence.service.CorrespondenceCancelService;
import com.gov.ac.correspondence.service.CorrespondenceCommentService;
import com.gov.ac.correspondence.service.CorrespondenceCreateService;
import com.gov.ac.correspondence.service.CorrespondenceDetailService;
import com.gov.ac.correspondence.service.CorrespondenceDraftReplyService;
import com.gov.ac.correspondence.service.CorrespondenceListService;
import com.gov.ac.correspondence.dto.CreateCorrespondenceCommentRequest;
import com.gov.ac.correspondence.dto.CorrespondenceCommentDetailDto;
import com.gov.ac.correspondence.service.CorrespondenceWorkflowActionService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
public class CorrespondenceController {

  private final CorrespondenceCreateService correspondenceCreateService;
  private final CorrespondenceDetailService correspondenceDetailService;
  private final CorrespondenceListService correspondenceListService;
  private final CorrespondenceWorkflowActionService correspondenceWorkflowActionService;
  private final CorrespondenceCommentService correspondenceCommentService;
  private final CorrespondenceCancelService correspondenceCancelService;
  private final CorrespondenceAttachmentMutationService correspondenceAttachmentMutationService;
  private final CorrespondenceDraftReplyService correspondenceDraftReplyService;

  @GetMapping
  public Page<CorrespondenceListItemDto> list(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) Instant createdFrom,
      @RequestParam(required = false) Instant createdTo) {
    return correspondenceListService.search(
        pageable, status, type, priority, createdFrom, createdTo, SecurityUtils.requireCurrentUserId());
  }

  @GetMapping("/{id}")
  public CorrespondenceDetailResponse getById(@PathVariable UUID id) {
    return correspondenceDetailService.getById(id, SecurityUtils.requireCurrentUserId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceCreatedResponse create(@Valid @RequestBody CorrespondenceCreateForm form) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    return correspondenceCreateService.create(userId, form);
  }

  /** Complete the current user's active Camunda task for this correspondence (same auth as view). */
  @PostMapping("/{id}/actions")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void workflowAction(
      @PathVariable UUID id, @RequestBody(required = false) WorkflowActionRequest body) {
    String action = body != null ? body.action() : null;
    String comment = body != null ? body.comment() : null;
    correspondenceWorkflowActionService.completeActiveAssigneeTask(
        id, SecurityUtils.requireCurrentUserId(), action, comment);
  }

  @PostMapping("/{id}/workflow-delegate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void workflowDelegate(
      @PathVariable UUID id, @Valid @RequestBody WorkflowDelegateRequest body) {
    correspondenceWorkflowActionService.delegateActiveAssigneeTask(
        id, SecurityUtils.requireCurrentUserId(), body.delegateeUserId());
  }

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceCommentDetailDto addComment(
      @PathVariable UUID id, @Valid @RequestBody CreateCorrespondenceCommentRequest request) {
    return correspondenceCommentService.addComment(
        id, SecurityUtils.requireCurrentUserId(), request);
  }

  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(
      @PathVariable UUID id,
      @RequestBody(required = false) @Valid CorrespondenceCancelRequest body) {
    correspondenceCancelService.cancel(
        id, SecurityUtils.requireCurrentUserId(), body != null ? body : new CorrespondenceCancelRequest(null));
  }

  @PostMapping("/{id}/attachments")
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceAttachmentDetailDto addAttachment(
      @PathVariable UUID id, @Valid @RequestBody CorrespondenceAttachmentForm form) {
    return correspondenceAttachmentMutationService.addAttachment(
        id, SecurityUtils.requireCurrentUserId(), form);
  }

  @PostMapping("/{id}/draft")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveDraft(
      @PathVariable UUID id,
      @RequestBody(required = false) @Valid CorrespondenceDraftSaveRequest body) {
    correspondenceDraftReplyService.saveDraft(
        id,
        SecurityUtils.requireCurrentUserId(),
        body != null ? body : new CorrespondenceDraftSaveRequest(""));
  }

  @PostMapping("/{id}/reply")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void sendReply(
      @PathVariable UUID id, @Valid @RequestBody CorrespondenceReplySendRequest body) {
    correspondenceDraftReplyService.sendReply(id, SecurityUtils.requireCurrentUserId(), body);
  }
}
