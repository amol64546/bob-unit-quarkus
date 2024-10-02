
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller;

import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "Message Listener Controller")
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "jwt")
@Path("/v1.0/trigger")
public interface MessageListenerController {

  @SecurityRequirement(name = "jwt")
  @Operation(summary = "Trigger Message Event")
  @APIResponses(
    value = {
      @APIResponse(responseCode = "200", description = "Success",
        content = {@Content(schema = @Schema(implementation = Map.class))}),
      @APIResponse(responseCode = "400", description = "Bad Request",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "401", description = "Unauthorized",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "404", description = "Not Found",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "503", description = "Service Unavailable",
        content = {@Content(schema = @Schema())})
    }
  )
  @Path("/message-listener")
  @POST
  ApiResponseBody triggerMessageEvent(@NotNull Object workflowEvent, @Context HttpHeaders httpHeaders);
}
