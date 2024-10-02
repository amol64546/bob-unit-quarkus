
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

/**
 * This class represents a signal exception.
 * It extends the RuntimeException class and includes constructors for different scenarios.
 * An instance of this class is thrown when an exception occurs while processing a signal.
 */
public class SignalException extends RuntimeException {

  public SignalException(Exception exception) {
    super(exception);
  }

  public SignalException(String errorMessage, Throwable exception) {
    super(errorMessage, exception);
  }

  public SignalException(String errorMessage) {
    super(errorMessage);
  }

}
