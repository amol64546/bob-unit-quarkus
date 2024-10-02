
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.exception;

import com.aidtaas.mobius.unit.constants.BobConstants;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Slf4j
public class BobRestClientResponseMapper implements ResponseExceptionMapper<Throwable> {


  @Override
  public Throwable toThrowable(Response response) {
    String responseAsString = BobConstants.NO_CONTENT_FROM_THE_RESPONSE;
    try {
      InputStream in = (InputStream) response.getEntity();
      if(ObjectUtils.isNotEmpty(in)) {
        byte[] bytes = in.readAllBytes();
        responseAsString = new String(bytes);
      }
    } catch (IOException e) {
      log.error("Error while reading response", e);
      throw new RetryableException(e.getMessage(), BobConstants.HTTP_STATUS_CODE_500);
    }
    if (response.getStatus() > BobConstants.HTTP_STATUS_CODE_500) {
      return new RetryableException(responseAsString, response.getStatus());
    } else {
      return new NonRetryableException(responseAsString, response.getStatus());
    }
  }
}
