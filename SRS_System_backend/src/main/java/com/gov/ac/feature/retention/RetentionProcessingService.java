package com.gov.ac.feature.retention;

import com.gov.ac.feature.retention.entity.ArchiveTransitionLogEntity;
import com.gov.ac.feature.retention.entity.RetentionPolicyEntity;
import com.gov.ac.feature.retention.repository.ArchiveTransitionLogRepository;
import com.gov.ac.feature.retention.repository.LegalHoldRepository;
import com.gov.ac.feature.retention.repository.RetentionPolicyRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hourly retention worker. Destructive paths are skipped entirely when {@link
 * RetentionProperties#dryRun()} is true — only {@code archive_transition_log} rows with action
 * {@code SKIPPED_DRY_RUN} are written. A blanket {@link com.gov.ac.feature.retention.entity.LegalHoldEntity}
 * (correspondence_id IS NULL) suppresses every mutating branch for the entire tick.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionProcessingService {

  public static final String APPLIES_AUDIT_EVENT = "AUDIT_EVENT";
  public static final String APPLIES_ACCESS_LOG = "ATTACHMENT_ACCESS_LOG";
  public static final String APPLIES_NOTIFICATION = "NOTIFICATION";
  public static final String APPLIES_DOWNLOAD_TOKEN = "ATTACHMENT_DOWNLOAD_TOKEN";
  public static final String APPLIES_DOCUMENT_SIGNATURE = "DOCUMENT_SIGNATURE";
  public static final String APPLIES_CORRESPONDENCE = "CORRESPONDENCE";
  public static final String APPLIES_ATTACHMENT_VERSION = "ATTACHMENT_VERSION";

  public static final String ACTION_HARD_DELETE = "HARD_DELETE";
  public static final String ACTION_ANONYMIZE = "ANONYMIZE";
  public static final String ACTION_SKIPPED_DRY_RUN = "SKIPPED_DRY_RUN";
  public static final String ACTION_SKIPPED_LEGAL_HOLD = "SKIPPED_LEGAL_HOLD";

  private final RetentionPolicyRepository policyRepository;
  private final LegalHoldRepository legalHoldRepository;
  private final ArchiveTransitionLogRepository archiveTransitionLogRepository;
  private final RetentionProperties retentionProperties;
  private final JdbcTemplate jdbcTemplate;
  private final EntityManager entityManager;

  @Transactional
  public void runTick() {
    if (legalHoldRepository.existsByCorrespondenceIsNullAndReleasedAtIsNullAndDeletedAtIsNull()) {
      log.info("Retention tick skipped: active blanket legal hold");
      return;
    }
    for (RetentionPolicyEntity policy : policyRepository.findByEnabledTrueAndDeletedAtIsNullOrderByCodeAsc()) {
      long lockKey =
          (policy.getId().getMostSignificantBits() ^ policy.getId().getLeastSignificantBits())
              & Long.MAX_VALUE;
      entityManager
          .createNativeQuery("SELECT pg_advisory_xact_lock(:k)")
          .setParameter("k", lockKey)
          .getSingleResult();
      try {
        processPolicy(policy);
      } catch (RuntimeException ex) {
        log.error("Retention policy {} failed: {}", policy.getCode(), ex.getMessage(), ex);
        writeLog(policy, null, "FAILED", "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
      }
    }
  }

  private void processPolicy(RetentionPolicyEntity policy) {
    Instant cutoff = Instant.now().minus(policy.getRetainForDays(), ChronoUnit.DAYS);
    int batch = retentionProperties.batchSize();
    boolean dry = retentionProperties.dryRun();

    int affected =
        switch (policy.getAppliesTo()) {
          case APPLIES_AUDIT_EVENT -> applyAudit(policy, cutoff, batch, dry);
          case APPLIES_ACCESS_LOG -> applyAccessLog(policy, cutoff, batch, dry);
          case APPLIES_NOTIFICATION -> applyNotification(policy, cutoff, batch, dry);
          case APPLIES_DOWNLOAD_TOKEN -> applyDownloadToken(policy, cutoff, batch, dry);
          case APPLIES_DOCUMENT_SIGNATURE -> applyDocumentSignature(policy, cutoff, batch, dry);
          case APPLIES_CORRESPONDENCE -> applyCorrespondence(policy, cutoff, batch, dry);
          case APPLIES_ATTACHMENT_VERSION -> applyAttachmentVersion(policy, cutoff, batch, dry);
          default -> 0;
        };
    if (affected <= 0) {
      return;
    }
    if (dry) {
      writeLog(policy, null, ACTION_SKIPPED_DRY_RUN, "{\"wouldMutateRows\":" + affected + "}");
    } else {
      writeLog(
          policy,
          null,
          policy.getActionAfter() != null ? policy.getActionAfter() : ACTION_HARD_DELETE,
          "{\"mutatedRows\":" + affected + "}");
    }
  }

  private int applyAudit(RetentionPolicyEntity policy, Instant cutoff, int batch, boolean dry) {
    if (!ACTION_HARD_DELETE.equals(policy.getActionAfter())) {
      return 0;
    }
    Integer cnt =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*)::int FROM srs_system.audit_event WHERE occurred_at < ?",
            Integer.class,
            ts(cutoff));
    int n = cnt == null ? 0 : cnt;
    if (dry) {
      return Math.min(n, batch);
    }
    return jdbcTemplate.update(
        """
        DELETE FROM srs_system.audit_event e
        USING (
          SELECT id FROM srs_system.audit_event
          WHERE occurred_at < ?
          ORDER BY occurred_at ASC
          LIMIT ?
        ) sub
        WHERE e.id = sub.id
        """,
        ts(cutoff),
        batch);
  }

  private int applyAccessLog(RetentionPolicyEntity policy, Instant cutoff, int batch, boolean dry) {
    if (!ACTION_HARD_DELETE.equals(policy.getActionAfter())) {
      return 0;
    }
    Integer cnt =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)::int FROM srs_system.attachment_access_log l
            WHERE l.occurred_at < ?
              AND NOT EXISTS (
                SELECT 1 FROM srs_system.legal_hold h
                WHERE h.correspondence_id = l.correspondence_id
                  AND h.released_at IS NULL AND h.deleted_at IS NULL
              )
            """,
            Integer.class,
            ts(cutoff));
    int n = cnt == null ? 0 : cnt;
    if (dry) {
      return Math.min(n, batch);
    }
    return jdbcTemplate.update(
        """
        DELETE FROM srs_system.attachment_access_log l
        USING (
          SELECT l2.id FROM srs_system.attachment_access_log l2
          WHERE l2.occurred_at < ?
            AND NOT EXISTS (
              SELECT 1 FROM srs_system.legal_hold h
              WHERE h.correspondence_id = l2.correspondence_id
                AND h.released_at IS NULL AND h.deleted_at IS NULL
            )
          ORDER BY l2.occurred_at ASC
          LIMIT ?
        ) sub
        WHERE l.id = sub.id
        """,
        ts(cutoff),
        batch);
  }

  private int applyNotification(RetentionPolicyEntity policy, Instant cutoff, int batch, boolean dry) {
    if (!ACTION_HARD_DELETE.equals(policy.getActionAfter())) {
      return 0;
    }
    Integer cnt =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*)::int FROM srs_system.notification WHERE created_at < ?",
            Integer.class,
            ts(cutoff));
    int n = cnt == null ? 0 : cnt;
    if (dry) {
      return Math.min(n, batch);
    }
    return jdbcTemplate.update(
        """
        DELETE FROM srs_system.notification n
        USING (
          SELECT id FROM srs_system.notification
          WHERE created_at < ?
          ORDER BY created_at ASC
          LIMIT ?
        ) sub
        WHERE n.id = sub.id
        """,
        ts(cutoff),
        batch);
  }

  private int applyDownloadToken(RetentionPolicyEntity policy, Instant cutoff, int batch, boolean dry) {
    if (!ACTION_HARD_DELETE.equals(policy.getActionAfter())) {
      return 0;
    }
    Integer cnt =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*)::int FROM srs_system.attachment_download_token WHERE expires_at < ?",
            Integer.class,
            ts(cutoff));
    int n = cnt == null ? 0 : cnt;
    if (dry) {
      return Math.min(n, batch);
    }
    return jdbcTemplate.update(
        """
        DELETE FROM srs_system.attachment_download_token t
        USING (
          SELECT id FROM srs_system.attachment_download_token
          WHERE expires_at < ?
          ORDER BY expires_at ASC
          LIMIT ?
        ) sub
        WHERE t.id = sub.id
        """,
        ts(cutoff),
        batch);
  }

  private int applyDocumentSignature(RetentionPolicyEntity policy, Instant cutoff, int batch, boolean dry) {
    if (!ACTION_HARD_DELETE.equals(policy.getActionAfter())) {
      return 0;
    }
    Integer cnt =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)::int FROM srs_system.document_signature s
            WHERE s.created_at < ?
            """,
            Integer.class,
            ts(cutoff));
    int n = cnt == null ? 0 : cnt;
    if (dry) {
      return Math.min(n, batch);
    }
    return jdbcTemplate.update(
        """
        DELETE FROM srs_system.document_signature s
        USING (
          SELECT id FROM srs_system.document_signature
          WHERE created_at < ?
          ORDER BY created_at ASC
          LIMIT ?
        ) sub
        WHERE s.id = sub.id
        """,
        ts(cutoff),
        batch);
  }

  private int applyCorrespondence(RetentionPolicyEntity policy, Instant cutoff, int batch, boolean dry) {
    if (ACTION_ANONYMIZE.equals(policy.getActionAfter())) {
      Integer cnt =
          jdbcTemplate.queryForObject(
              """
              SELECT COUNT(*)::int FROM srs_system.correspondence c
              WHERE c.deleted_at IS NOT NULL AND c.deleted_at < ?
                AND NOT EXISTS (
                  SELECT 1 FROM srs_system.legal_hold h
                  WHERE h.correspondence_id = c.id
                    AND h.released_at IS NULL AND h.deleted_at IS NULL
                )
              """,
              Integer.class,
              ts(cutoff));
      int n = cnt == null ? 0 : cnt;
      if (dry) {
        return Math.min(n, batch);
      }
      return jdbcTemplate.update(
          """
          UPDATE srs_system.correspondence c
          SET subject = '[REDACTED]',
              description = NULL,
              body_html = NULL,
              reply_draft_html = NULL,
              updated_at = now()
          FROM (
            SELECT id FROM srs_system.correspondence c2
            WHERE c2.deleted_at IS NOT NULL AND c2.deleted_at < ?
              AND NOT EXISTS (
                SELECT 1 FROM srs_system.legal_hold h
                WHERE h.correspondence_id = c2.id
                  AND h.released_at IS NULL AND h.deleted_at IS NULL
              )
            ORDER BY c2.deleted_at ASC
            LIMIT ?
          ) sub
          WHERE c.id = sub.id
          """,
          ts(cutoff),
          batch);
    }
    if (ACTION_HARD_DELETE.equals(policy.getActionAfter())) {
      Integer cnt =
          jdbcTemplate.queryForObject(
              """
              SELECT COUNT(*)::int FROM srs_system.correspondence c
              WHERE c.deleted_at IS NOT NULL AND c.deleted_at < ?
                AND NOT EXISTS (
                  SELECT 1 FROM srs_system.legal_hold h
                  WHERE h.correspondence_id = c.id
                    AND h.released_at IS NULL AND h.deleted_at IS NULL
                )
              """,
              Integer.class,
              ts(cutoff));
      int n = cnt == null ? 0 : cnt;
      if (dry) {
        return Math.min(n, batch);
      }
      return jdbcTemplate.update(
          """
          DELETE FROM srs_system.correspondence c
          USING (
            SELECT id FROM srs_system.correspondence c2
            WHERE c2.deleted_at IS NOT NULL AND c2.deleted_at < ?
              AND NOT EXISTS (
                SELECT 1 FROM srs_system.legal_hold h
                WHERE h.correspondence_id = c2.id
                  AND h.released_at IS NULL AND h.deleted_at IS NULL
              )
            ORDER BY c2.deleted_at ASC
            LIMIT ?
          ) sub
          WHERE c.id = sub.id
          """,
          ts(cutoff),
          batch);
    }
    return 0;
  }

  private int applyAttachmentVersion(RetentionPolicyEntity policy, Instant cutoff, int batch, boolean dry) {
    if (!ACTION_HARD_DELETE.equals(policy.getActionAfter())) {
      return 0;
    }
    Integer cnt =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)::int FROM srs_system.attachment_version v
            JOIN srs_system.attachment a ON a.id = v.attachment_id
            WHERE v.deleted_at IS NOT NULL AND v.deleted_at < ?
              AND NOT EXISTS (
                SELECT 1 FROM srs_system.legal_hold h
                WHERE h.correspondence_id = a.correspondence_id
                  AND h.released_at IS NULL AND h.deleted_at IS NULL
              )
            """,
            Integer.class,
            ts(cutoff));
    int n = cnt == null ? 0 : cnt;
    if (dry) {
      return Math.min(n, batch);
    }
    return jdbcTemplate.update(
        """
        DELETE FROM srs_system.attachment_version v
        USING (
          SELECT v2.id FROM srs_system.attachment_version v2
          JOIN srs_system.attachment a ON a.id = v2.attachment_id
          WHERE v2.deleted_at IS NOT NULL AND v2.deleted_at < ?
            AND NOT EXISTS (
              SELECT 1 FROM srs_system.legal_hold h
              WHERE h.correspondence_id = a.correspondence_id
                AND h.released_at IS NULL AND h.deleted_at IS NULL
            )
          ORDER BY v2.deleted_at ASC
          LIMIT ?
        ) sub
        WHERE v.id = sub.id
        """,
        ts(cutoff),
        batch);
  }

  private void writeLog(RetentionPolicyEntity policy, UUID resourceId, String action, String detailJson) {
    ArchiveTransitionLogEntity row = new ArchiveTransitionLogEntity();
    row.setAppliedTo(policy.getAppliesTo());
    row.setResourceId(resourceId != null ? resourceId.toString() : "BATCH");
    row.setPolicy(policy);
    row.setAction(action);
    row.setExecutedAt(Instant.now());
    row.setDetailJson(detailJson);
    archiveTransitionLogRepository.save(row);
  }

  private static Timestamp ts(Instant instant) {
    return Timestamp.from(instant);
  }

  private static String escapeJson(String msg) {
    if (msg == null) {
      return "";
    }
    return msg.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
