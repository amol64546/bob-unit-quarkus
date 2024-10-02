
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

public class DataCountRetrievalException extends RuntimeException {

  public DataCountRetrievalException(Exception exception) {
    super(exception);
  }

  public DataCountRetrievalException(String errorMessage, Exception exception) {
    super(errorMessage, exception);
  }

  public DataCountRetrievalException(String errorMessage) {
    super(errorMessage);
  }
}
