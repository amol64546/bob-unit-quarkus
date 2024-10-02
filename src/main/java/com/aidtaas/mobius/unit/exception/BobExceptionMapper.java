
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.error.BobError;
import com.aidtaas.mobius.unit.error.CommonError;
import com.aidtaas.mobius.error.services.error.CommonErrors;
import com.aidtaas.mobius.error.services.error.Error;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

@Provider
@Slf4j
public class BobExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable e) {
    Response errorResponse;
    if (e instanceof PersistenceException) {
      var error = new BobError(CommonError.JDBC_CONNECTION_FAIL, List.of(e.getMessage()));
      errorResponse = Response.status(SERVICE_UNAVAILABLE).entity(error).build();
    } else if (e instanceof ProcessingException || e instanceof HttpHostConnectException) {
      var error = new BobError(CommonError.HTTP_CONNECTION_FAIL, List.of(e.getMessage()));
      errorResponse = Response.status(SERVICE_UNAVAILABLE).entity(error).build();
    } else if (e instanceof ValidationException) {
      var error = new Error(HttpStatus.BAD_REQUEST, BobConstants.BAD_REQUEST_0,
        e.getMessage(),
        "Please verify the request and try again.");
      errorResponse = Response.status(BAD_REQUEST).entity(error).build();
    } else if (e instanceof ApiException) {
      var error = new Error(HttpStatus.INTERNAL_SERVER_ERROR, BobConstants.INTERNAL_ERROR_1,
        e.getMessage(),
        "Please try again after sometime or contact the support team.");
      errorResponse = Response.status(INTERNAL_SERVER_ERROR).entity(error).build();
    } else if (e instanceof AuthorizationException) {
      var error = new Error(HttpStatus.UNAUTHORIZED, BobConstants.UNAUTHORIZED,
        e.getMessage(),
        "Please ensure the correct token is included to execute the API request.");
      errorResponse = Response.status(UNAUTHORIZED).entity(error).build();
    } else if (e instanceof NotSupportedException) {
      var error = new BobError(CommonError.NOT_SUPPORTED_EXCEPTION, List.of(e.getMessage()));
      errorResponse = Response.status(BAD_REQUEST).entity(error).build();
    } else {
      var error = new BobError(CommonErrors.UNEXPECTED_ERROR, List.of(e.getMessage()));
      errorResponse = Response.status(INTERNAL_SERVER_ERROR).entity(error).build();
    }
    return errorResponse;
  }
}
