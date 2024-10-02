
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

/**
 * This class represents an output parse exception.
 * It extends the NonRetryableException class and includes constructors for different scenarios.
 * An instance of this class is thrown when an exception occurs while parsing the output.
 */
public class OutputParseException extends NonRetryableException {

  public OutputParseException(Exception exception) {
    super(exception);
  }

  public OutputParseException(String errorMessage, Throwable exception) {
    super(errorMessage, exception);
  }

  public OutputParseException(String errorMessage) {
    super(errorMessage);
  }
}
