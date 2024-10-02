
/*
 * Gaian Copyright
 * Copyright (C) : Gaian Solutions Ltd
 */

package com.aidtaas.mobius.unit.error;

import com.aidtaas.mobius.error.services.error.CommonErrors;
import com.aidtaas.mobius.error.services.error.Error;
import org.springframework.http.HttpStatus;

public final class CommonError extends CommonErrors {

  public static final Error JDBC_CONNECTION_FAIL =
    new Error(HttpStatus.SERVICE_UNAVAILABLE, 503000, "We are facing technical issue, Please try again after sometime",
      "Make sure POSTGRES is up and running");

  public static final Error HTTP_CONNECTION_FAIL =
    new Error(HttpStatus.SERVICE_UNAVAILABLE, 503001, "We are facing technical issue, Please try again after sometime",
      "Make sure all dependent services are up and running");

  public static final Error MALFORMED_URL =
    new Error(HttpStatus.INTERNAL_SERVER_ERROR, 500001, "Malformed url", "");

  public static final Error HTTP_METHOD_ERROR =
    new Error(HttpStatus.INTERNAL_SERVER_ERROR, 500002, "Unexpected value in httpMethod",
      "");

  public static final Error NOT_SUPPORTED_EXCEPTION =
    new Error(HttpStatus.BAD_REQUEST, 400001, "Unsupported body content type",
      "Please provide the body of required content type and try again.");

  public static final Error FORM_DATA_PROCESSING_ERROR =
    new Error(HttpStatus.BAD_REQUEST, 400002, "Error processing input form data",
      "Please check the input form data to be processed");


  private CommonError() {
  }
}
