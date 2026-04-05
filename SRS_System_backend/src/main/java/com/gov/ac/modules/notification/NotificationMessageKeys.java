package com.gov.ac.modules.notification;

/**
 * i18n keys for {@link com.gov.ac.domain.notification.InAppNotification#getMessageKey()}; resolved in
 * the Angular app (or other client), not as hardcoded server text.
 */
public final class NotificationMessageKeys {

  private NotificationMessageKeys() {}

  public static final String CORRESPONDENCE_CREATED = "notification.correspondence.created";

  public static final String CORRESPONDENCE_COMMENT_ADDED = "notification.correspondence.comment_added";

  public static final String WORKFLOW_TASK_COMPLETED = "notification.workflow.task_completed";
}
