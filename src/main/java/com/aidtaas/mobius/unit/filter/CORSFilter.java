
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

  /**
   * Filter method called after a response has been provided for a request (either by a normal resource method or by
   * a request filter).
   *
   * @param requestContext  request context.
   * @param responseContext response context.
   */
  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    responseContext.getHeaders().add("Access-Control-Allow-Origin", requestContext.getHeaderString("Origin"));
    responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
    responseContext.getHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
    responseContext.getHeaders().add("Access-Control-Max-Age", "3600");
    responseContext.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With, " +
      "remember-me, Authorization");
  }
}
