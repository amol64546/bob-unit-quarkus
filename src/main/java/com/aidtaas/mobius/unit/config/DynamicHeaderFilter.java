
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.util.Collections;
import java.util.Map;

public class DynamicHeaderFilter implements ClientRequestFilter {

  private final Map<String, String> headers;

  public DynamicHeaderFilter(Map<String, String> headers) {
    this.headers = Collections.unmodifiableMap(headers);
  }

  @Override
  public void filter(ClientRequestContext requestContext) {
    if (headers != null) {
      headers.forEach(requestContext.getHeaders()::add);
    }
  }
}
