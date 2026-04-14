package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.CorrespondenceAggregateLimits;
import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.correspondence.dto.CorrespondenceDraftSaveRequestDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceReplySendRequestDto;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.shared.notification.service.NotificationService;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CorrespondenceDraftReplyService {

  private static final DateTimeFormatter TS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final CorrespondenceActionAudit correspondenceActionAudit;
  private final NotificationService notificationService;

  @Transactional
  public void saveDraft(UUID correspondenceId, UUID actorUserId, CorrespondenceDraftSaveRequestDto body) {
    String raw = body.bodyHtml() != null ? body.bodyHtml() : "";
    assertHtmlLength(raw);
    CorrespondenceEntity c = loadMutable(correspondenceId, actorUserId);
    String trimmed = raw.trim();
    c.setReplyDraftHtml(trimmed.isEmpty() ? null : trimmed);
    c.setUpdatedBy(actorUserId);
    correspondenceRepository.save(c);

    Map<String, Object> audit = new HashMap<>();
    audit.put("chars", trimmed.length());
    audit.put("cleared", trimmed.isEmpty());
    correspondenceActionAudit.log(
        actorUserId, CorrespondenceActionAudit.ACTION_DRAFT_SAVE, correspondenceId, audit);
  }

  @Transactional
  public void sendReply(UUID correspondenceId, UUID actorUserId, CorrespondenceReplySendRequestDto body) {
    assertHtmlLength(body.bodyHtml());
    CorrespondenceEntity c = loadMutable(correspondenceId, actorUserId);
    AppUserEntity actor =
        appUserRepository.findByIdAndDeletedAtIsNull(actorUserId).orElseThrow();

    String addition = body.bodyHtml().trim();
    String header =
        "<hr/><p><em>Reply sent "
            + TS.format(Instant.now())
            + " — user "
            + actor.getUsername()
            + "</em></p>";
    String existing = c.getBodyHtml() != null ? c.getBodyHtml() : "";
    c.setBodyHtml(existing + header + addition);
    c.setReplyDraftHtml(null);
    c.setUpdatedBy(actorUserId);
    correspondenceRepository.save(c);

    notificationService.notifyCommentAdded(c, actor);

    Map<String, Object> audit = new HashMap<>();
    audit.put("appendedChars", addition.length());
    correspondenceActionAudit.log(
        actorUserId, CorrespondenceActionAudit.ACTION_REPLY_SENT, correspondenceId, audit);
  }

  private CorrespondenceEntity loadMutable(UUID correspondenceId, UUID actorUserId) {
    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot edit this correspondence"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new ForbiddenException("You cannot edit this correspondence");
    }
    CorrespondenceEntity c =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    correspondenceViewAuthorization.assertCanView(actor, c);
    CorrespondenceMutationGuards.assertCorrespondenceMutable(c);
    return c;
  }

  private static void assertHtmlLength(String html) {
    if (html != null && html.length() > CorrespondenceAggregateLimits.MAX_HTML_CHARS) {
      throw new com.gov.ac.common.api.BadRequestException(
          "HTML content exceeds max length " + CorrespondenceAggregateLimits.MAX_HTML_CHARS);
    }
  }
}
