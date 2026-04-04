package com.gov.ac.common.api;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<String> notFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<String> badRequest(BadRequestException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<String> forbidden(ForbiddenException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<String> accessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
  }

  @ExceptionHandler(SystemConfigurationException.class)
  ResponseEntity<String> systemConfiguration(SystemConfigurationException ex) {
    log.error("System configuration / seed data error: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Server configuration error. Contact the administrator.");
  }

  @ExceptionHandler(ProcessEngineException.class)
  ResponseEntity<String> processEngine(ProcessEngineException ex) {
    log.error("Camunda process engine error", ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body("Workflow engine could not complete the operation. Please try again later.");
  }

  @ExceptionHandler(UncheckedIOException.class)
  ResponseEntity<String> uncheckedIo(UncheckedIOException ex) {
    log.error("IO error streaming response", ex.getCause());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body("Could not transfer attachment content. Please try again later.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<String> dataIntegrity(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body("The request could not be completed due to a data conflict.");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                    (a, b) -> a));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<String> badCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<String> authentication(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
  }
}
