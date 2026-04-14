package com.gov.ac.feature.correspondence.reference;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Year;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReferenceNumberGenerator {

  private static final String SEQ_SQL = "SELECT nextval('srs_system.correspondence_reference_seq')";

  @PersistenceContext private EntityManager entityManager;

  /**
   * Registry-style number, e.g. {@code CORR-2026-00000001}. {@code nextval} is atomic per PostgreSQL
   * session/transaction, so concurrent callers receive distinct sequence values. Uniqueness among
   * non-deleted rows is also enforced by {@code ux_correspondence_reference_active} (Flyway V6).
   */
  public String nextReferenceNumber() {
    long seq =
        ((Number) entityManager.createNativeQuery(SEQ_SQL).getSingleResult()).longValue();
    int year = Year.now(ZoneOffset.UTC).getValue();
    String ref = String.format("CORR-%d-%08d", year, seq);
    log.debug("Allocated reference_number seq={} -> {}", seq, ref);
    return ref;
  }
}
