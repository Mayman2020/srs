package com.gov.ac.common.api;

/** HTTP 410 Gone — used for expired / revoked public verification tokens. */
public class GoneException extends RuntimeException {
  public GoneException(String message) {
    super(message);
  }
}
