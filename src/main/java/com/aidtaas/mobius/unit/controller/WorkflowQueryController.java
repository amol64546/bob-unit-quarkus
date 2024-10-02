
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "Workflow Query Controller")
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "jwt")
@Path("/v1.0/wf")
public interface WorkflowQueryController {

  @SecurityRequirement(name = "jwt")
  @Operation(summary = "Get suspended process definition count for each workflow")
  @APIResponses(
    value = {
      @APIResponse(responseCode = "200", description = "Success",
        content = {@Content(schema = @Schema(implementation = Map.class))}),
      @APIResponse(responseCode = "400", description = "Bad Request",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "401", description = "Unauthorized",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "503", description = "Service Unavailable",
        content = {@Content(schema = @Schema())})
    }
  )
  @POST
  @Path("/process-definition/suspended/filter")
  Response getSuspendedProcessDefinitionCountForEachWfs(@NotEmpty List<String> wfIds, @Context HttpHeaders httpHeaders);


  @SecurityRequirement(name = "jwt")
  @Operation(summary = "Get suspended process definition count for given workflow and deployed version")
  @APIResponses(
    value = {
      @APIResponse(responseCode = "200", description = "Success",
        content = {@Content(schema = @Schema(implementation = Long.class))}),
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
  @GET
  @Path("/process-definition/suspended")
  Response getSuspendedProcDefCount(@NotNull @QueryParam("wfId") String wfId,
                                    @NotNull @QueryParam("deployedVersion") Integer deployedVersion,
                                    @Context HttpHeaders httpHeaders);


  @SecurityRequirement(name = "jwt")
  @Operation(summary = "Get activity data count for given workflow and process instance")
  @APIResponses(
    value = {
      @APIResponse(responseCode = "200", description = "Success",
        content = {@Content(schema = @Schema(implementation = Map.class))}),
      @APIResponse(responseCode = "400", description = "Bad Request",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "401", description = "Unauthorized",
        content = {@Content(schema = @Schema())}),
      @APIResponse(responseCode = "503", description = "Service Unavailable",
        content = {@Content(schema = @Schema())})
    }
  )
  @GET
  @Path("/activity/data/count")
  Response getActivityDataCount(@NotNull @QueryParam("wfId") String wfId,
                                @NotNull @QueryParam("processInstanceId") String processInstanceId,
                                @Context HttpHeaders httpHeaders);
}
