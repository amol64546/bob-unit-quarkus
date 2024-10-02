
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

public class AuthorizationException extends RuntimeException {

  public AuthorizationException(Exception exception) {
    super(exception);
  }

  public AuthorizationException(String errorMessage, Exception exception) {
    super(errorMessage, exception);
  }

  public AuthorizationException(String errorMessage) {
    super(errorMessage);
  }
}
