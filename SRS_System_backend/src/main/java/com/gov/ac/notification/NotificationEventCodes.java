package com.gov.ac.notification;

/** Must match {@code notification_event_type.code} (Flyway seeds). */
public final class NotificationEventCodes {

  private NotificationEventCodes() {}

  public static final String CORRESPONDENCE_CREATED = "CORRESPONDENCE_CREATED";
  public static final String COMMENT_ADDED = "COMMENT_ADDED";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";
  public static final String RETURNED = "RETURNED";
  public static final String ASSIGNED = "ASSIGNED";
}
