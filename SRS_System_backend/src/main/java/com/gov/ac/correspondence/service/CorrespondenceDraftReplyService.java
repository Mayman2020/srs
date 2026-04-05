package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.CorrespondenceAggregateLimits;
import com.gov.ac.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.correspondence.dto.CorrespondenceDraftSaveRequest;
import com.gov.ac.correspondence.dto.CorrespondenceReplySendRequest;
import com.gov.ac.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.modules.notification.NotificationService;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
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
  public void saveDraft(UUID correspondenceId, UUID actorUserId, CorrespondenceDraftSaveRequest body) {
    String raw = body.bodyHtml() != null ? body.bodyHtml() : "";
    assertHtmlLength(raw);
    Correspondence c = loadMutable(correspondenceId, actorUserId);
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
  public void sendReply(UUID correspondenceId, UUID actorUserId, CorrespondenceReplySendRequest body) {
    assertHtmlLength(body.bodyHtml());
    Correspondence c = loadMutable(correspondenceId, actorUserId);
    AppUser actor =
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

  private Correspondence loadMutable(UUID correspondenceId, UUID actorUserId) {
    AppUser actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot edit this correspondence"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new ForbiddenException("You cannot edit this correspondence");
    }
    Correspondence c =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("Correspondence not found"));
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
