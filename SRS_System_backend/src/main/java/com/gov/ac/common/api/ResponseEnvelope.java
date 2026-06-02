package com.gov.ac.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Unified success/error envelope returned by every {@code /api/v1/*} endpoint.
 *
 * <p>Mirrors the Inteanet {@code ResponsePojo} pattern: every response carries a {@code traceId},
 * a deterministic {@code success} flag, optional {@code errorCode}, and a single typed {@code
 * data} payload. Error responses additionally carry a {@code message} and an {@code errors} map
 * with field-level validation hints. Controllers may continue returning raw DTOs while we phase
 * in the envelope — {@link RestExceptionHandler} guarantees errors always use this shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseEnvelope<T>(
    String traceId,
    Instant timestamp,
    boolean success,
    String errorCode,
    String message,
    T data) {

  public static <T> ResponseEnvelope<T> ok(T data) {
    return new ResponseEnvelope<>(newTraceId(), Instant.now(), true, null, null, data);
  }

  public static <T> ResponseEnvelope<T> ok(T data, String message) {
    return new ResponseEnvelope<>(newTraceId(), Instant.now(), true, null, message, data);
  }

  public static ResponseEnvelope<Void> error(String errorCode, String message) {
    return new ResponseEnvelope<>(newTraceId(), Instant.now(), false, errorCode, message, null);
  }

  public static <T> ResponseEnvelope<T> error(String errorCode, String message, T data) {
    return new ResponseEnvelope<>(newTraceId(), Instant.now(), false, errorCode, message, data);
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString();
  }
}
