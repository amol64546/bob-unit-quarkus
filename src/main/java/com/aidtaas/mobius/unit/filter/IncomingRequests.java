
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.filter;

import com.aidtaas.mobius.unit.aclmodels.enums.RequesterType;
import com.aidtaas.mobius.unit.exception.AuthorizationException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static com.aidtaas.mobius.unit.constants.BobConstants.*;


@Provider
@ApplicationScoped
@Slf4j
public class IncomingRequests implements ContainerRequestFilter {

  /**
   * Filter to extract tenantId or userId from the token and add it to the request headers
   *
   * @param requestContext
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {

    var auth = requestContext.getHeaderString(AUTHORIZATION);

    if (StringUtils.isNotEmpty(auth) && auth.startsWith(BEARER)) {
      var token = auth.substring(BEARER.length());
      try {
        DecodedJWT decodedToken = JWT.decode(token);
        var tenantId = decodedToken.getClaim(TENANT_ID).asString();
        var userId = decodedToken.getClaim(USER_ID).asString();

        if (StringUtils.isNotEmpty(tenantId)) {
          requestContext.getHeaders().add(REQUESTER_ID, tenantId);
          requestContext.getHeaders().add(REQUESTER_TYPE, RequesterType.TENANT.name());
        } else if (StringUtils.isNotEmpty(userId)) {
          requestContext.getHeaders().add(REQUESTER_ID, userId);
          requestContext.getHeaders().add(REQUESTER_TYPE, RequesterType.TENANT_USER.name());
        } else {
          log.error("No tenantId or userId found in token");
          throw new AuthorizationException("Authorization Token is invalid, no tenantId or userId found in token");
        }
      } catch (Exception e) {
        log.error("Error while decoding token: {}", e.getMessage());
        throw new AuthorizationException("Tenant does not have access to the constructs.");
      }
    } else {
      throw new AuthorizationException("The API execution cannot proceed without a provided token.");
    }
  }
}
