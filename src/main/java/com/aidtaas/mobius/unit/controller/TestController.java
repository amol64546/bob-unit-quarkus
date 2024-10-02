
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/v1.2")
public interface TestController {

  @Operation(summary = "Test Service", hidden = true)
  @APIResponses(
    value = {
      @APIResponse(responseCode = "200", description = "Success",
        content = {@Content(schema = @Schema(implementation = String.class))}),
      @APIResponse(responseCode = "401", description = "Unauthorized",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "503", description = "Service Unavailable",
        content = {@Content(schema = @Schema())})
    }
  )
  @GET
  Response testService();
}
