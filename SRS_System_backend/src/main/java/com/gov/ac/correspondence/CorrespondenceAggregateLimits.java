package com.gov.ac.correspondence;

/** Application-enforced aggregate limits (SRS §14.1 style). */
public final class CorrespondenceAggregateLimits {

  private CorrespondenceAggregateLimits() {}

  public static final long MAX_TOTAL_ATTACHMENT_BYTES = 200L * 1024 * 1024;
  public static final int MAX_ATTACHMENTS_COUNT = 30;
  public static final int MAX_HTML_CHARS = 500_000;
}
