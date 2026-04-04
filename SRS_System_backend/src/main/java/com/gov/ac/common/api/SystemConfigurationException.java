package com.gov.ac.common.api;

/**
 * Thrown when required seed data or environment configuration is missing (operator fault), not a
 * client validation error.
 */
public class SystemConfigurationException extends RuntimeException {

  public SystemConfigurationException(String message) {
    super(message);
  }
}
