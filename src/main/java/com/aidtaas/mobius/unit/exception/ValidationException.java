
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

/**
 * This class represents a validation exception.
 * It extends the NonRetryableException class and includes constructors for different scenarios.
 * An instance of this class is thrown when an exception occurs while validating the input.
 */
public class ValidationException extends NonRetryableException {

  public ValidationException(Exception exception) {
    super(exception);
  }

  public ValidationException(String errorMessage, Exception exception) {
    super(errorMessage, exception);
  }

  public ValidationException(String errorMessage) {
    super(errorMessage);
  }
}
