package com.gov.ac.common.api;

import com.gov.ac.common.i18n.Messages;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Centralized exception advice producing RFC 7807 problem+json responses with i18n messages.
 *
 * <p>All errors return {@link ProblemDetail} so the front-end has a stable shape (status, type,
 * title, detail, instance, plus {@code errorCode} / {@code errors} extensions). The body type is
 * {@code application/problem+json}.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler {

  private static final URI TYPE_GENERIC = URI.create("about:blank");
  private final Messages messages;

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ProblemDetail> notFound(NotFoundException ex, WebRequest request) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
        coalesce(ex.getMessage(), messages.get("error.notFound")), request);
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<ProblemDetail> badRequest(BadRequestException ex, WebRequest request) {
    return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
        coalesce(ex.getMessage(), messages.get("error.badRequest")), request);
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<ProblemDetail> forbidden(ForbiddenException ex, WebRequest request) {
    return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
        coalesce(ex.getMessage(), messages.get("error.forbidden")), request);
  }

  @ExceptionHandler(GoneException.class)
  ResponseEntity<ProblemDetail> gone(GoneException ex, WebRequest request) {
    return build(HttpStatus.GONE, "GONE", coalesce(ex.getMessage(), messages.get("error.gone")), request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> accessDenied(AccessDeniedException ex, WebRequest request) {
    return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", messages.get("error.forbidden"), request);
  }

  @ExceptionHandler(SystemConfigurationException.class)
  ResponseEntity<ProblemDetail> systemConfiguration(
      SystemConfigurationException ex, WebRequest request) {
    log.error("System configuration / seed data error: {}", ex.getMessage());
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM_CONFIG",
        messages.get("error.systemConfig"), request);
  }

  @ExceptionHandler(ProcessEngineException.class)
  ResponseEntity<ProblemDetail> processEngine(ProcessEngineException ex, WebRequest request) {
    log.error("Camunda process engine error", ex);
    return build(HttpStatus.BAD_GATEWAY, "WORKFLOW_ENGINE",
        messages.get("error.workflowEngine"), request);
  }

  @ExceptionHandler(UncheckedIOException.class)
  ResponseEntity<ProblemDetail> uncheckedIo(UncheckedIOException ex, WebRequest request) {
    log.error("IO error streaming response", ex.getCause());
    return build(HttpStatus.BAD_GATEWAY, "ATTACHMENT_TRANSFER",
        messages.get("error.attachmentTransfer"), request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ProblemDetail> dataIntegrity(
      DataIntegrityViolationException ex, WebRequest request) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return build(HttpStatus.CONFLICT, "DATA_CONFLICT",
        messages.get("error.conflict"), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(
      MethodArgumentNotValidException ex, WebRequest request) {
    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                    (a, b) -> a,
                    LinkedHashMap::new));
    ProblemDetail problem = baseProblem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
        messages.get("error.validation"), request);
    problem.setProperty("errors", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ProblemDetail> badCredentials(
      BadCredentialsException ex, WebRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
        coalesce(ex.getMessage(), messages.get("auth.invalidCredentials")), request);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ProblemDetail> authentication(
      AuthenticationException ex, WebRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
        coalesce(ex.getMessage(), messages.get("error.unauthorized")), request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> generic(Exception ex, WebRequest request) {
    log.error("Unhandled server error", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL",
        messages.get("error.generic"), request);
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private ProblemDetail baseProblem(
      HttpStatus status, String errorCode, String detail, WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(TYPE_GENERIC);
    problem.setTitle(status.getReasonPhrase());
    problem.setProperty("errorCode", errorCode);
    String descriptor = request.getDescription(false); // e.g. uri=/api/v1/...
    if (descriptor != null && descriptor.startsWith("uri=")) {
      problem.setInstance(URI.create(descriptor.substring("uri=".length())));
    }
    return problem;
  }

  private ResponseEntity<ProblemDetail> build(
      HttpStatus status, String errorCode, String detail, WebRequest request) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(baseProblem(status, errorCode, detail, request));
  }

  private static String coalesce(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }
}
