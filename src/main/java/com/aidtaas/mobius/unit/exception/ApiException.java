
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

/**
 * This class represents an API exception.
 * It extends the RuntimeException class and includes constructors for different scenarios.
 * An instance of this class is thrown when an exception occurs while processing an API request.
 */
public class ApiException extends RuntimeException {

  /**
   * Constructor for the ApiException class.
   * It initializes the cause of the exception.
   *
   * @param exception the cause of the exception
   */
  public ApiException(Exception exception) {
    super(exception);
  }

  /**
   * Constructor for the ApiException class.
   * It initializes the message and cause of the exception.
   *
   * @param errorMessage the message of the exception
   * @param exception the cause of the exception
   */
  public ApiException(String errorMessage, Exception exception) {
    super(errorMessage, exception);
  }

  /**
   * Constructor for the ApiException class.
   * It initializes the message of the exception.
   *
   * @param errorMessage the message of the exception
   */
  public ApiException(String errorMessage) {
    super(errorMessage);
  }
}
