
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;


public class StateLockException extends RuntimeException {

  public StateLockException(Exception exception) {
    super(exception);
  }

  public StateLockException(String errorMessage, Throwable exception) {
    super(errorMessage, exception);
  }

  public StateLockException(String errorMessage) {
    super(errorMessage);
  }
}
