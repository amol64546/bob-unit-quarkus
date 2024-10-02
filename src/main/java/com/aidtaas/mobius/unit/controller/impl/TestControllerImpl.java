
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller.impl;

import com.aidtaas.mobius.unit.controller.TestController;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TestControllerImpl implements TestController {

  /**
   * Test service.
   *
   * @return The response.
   */
  @Override
  public Response testService() {
    log.info("POST:/v1.2");
    return Response.ok("Hello from Bob-Camunda !!").build();
  }
}
