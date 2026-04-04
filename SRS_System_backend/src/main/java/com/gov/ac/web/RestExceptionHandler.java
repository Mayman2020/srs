package com.gov.ac.web;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
