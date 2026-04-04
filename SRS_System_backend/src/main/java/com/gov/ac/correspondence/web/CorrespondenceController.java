package com.gov.ac.correspondence.web;

import com.gov.ac.correspondence.dto.CorrespondenceCreateForm;
import com.gov.ac.correspondence.dto.CorrespondenceCreatedResponse;
import com.gov.ac.correspondence.dto.CorrespondenceDetailResponse;
import com.gov.ac.correspondence.dto.CorrespondenceListItemDto;
import com.gov.ac.correspondence.dto.WorkflowActionRequest;
import com.gov.ac.correspondence.service.CorrespondenceCreateService;
import com.gov.ac.correspondence.service.CorrespondenceDetailService;
import com.gov.ac.correspondence.service.CorrespondenceListService;
import com.gov.ac.correspondence.dto.CreateCorrespondenceCommentRequest;
import com.gov.ac.correspondence.dto.CorrespondenceCommentDetailDto;
import com.gov.ac.correspondence.service.CorrespondenceCommentService;
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

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceCommentDetailDto addComment(
      @PathVariable UUID id, @Valid @RequestBody CreateCorrespondenceCommentRequest request) {
    return correspondenceCommentService.addComment(
        id, SecurityUtils.requireCurrentUserId(), request);
  }
}
