package com.gov.ac.feature.dashboard;

/**
 * Values stored in {@code correspondence_status.kpi_segment} (Flyway CHECK + application queries).
 * Not business lifecycle codes — reporting buckets only.
 */
public final class KpiSegmentCodes {

  private KpiSegmentCodes() {}

  public static final String SLA_DONE = "SLA_DONE";
  public static final String PIPELINE = "PIPELINE";
  public static final String INBOX = "INBOX";
}
