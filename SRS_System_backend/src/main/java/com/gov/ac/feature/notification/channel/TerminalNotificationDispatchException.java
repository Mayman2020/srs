package com.gov.ac.feature.notification.channel;

/**
 * Non-retryable dispatch failure (e.g. HTTP 4xx on a webhook). The outbox worker marks the row
 * DEAD without further backoff attempts.
 */
public class TerminalNotificationDispatchException extends RuntimeException {

  public TerminalNotificationDispatchException(String message) {
    super(message);
  }

  public TerminalNotificationDispatchException(String message, Throwable cause) {
    super(message, cause);
  }
}
