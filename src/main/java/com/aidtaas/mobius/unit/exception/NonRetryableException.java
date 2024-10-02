
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

/**
 * This class represents a non-retryable exception.
 * It extends the RuntimeException class and includes constructors for different scenarios.
 * An instance of this class is thrown when an exception occurs that should not be retried.
 */
public class NonRetryableException extends RuntimeException {

  private final int statusCode;

  public NonRetryableException(String errorMessage, int statusCode) {
    super(errorMessage);
    this.statusCode = statusCode;
  }

  public NonRetryableException(Exception exception) {
    super(exception);
    this.statusCode = 0;
  }

  public NonRetryableException(String errorMessage, Throwable exception) {
    super(errorMessage, exception);
    this.statusCode = 0;
  }

  public NonRetryableException(String errorMessage) {
    super(errorMessage);
    this.statusCode = 0;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
