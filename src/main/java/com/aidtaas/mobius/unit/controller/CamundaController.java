
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller;

import com.aidtaas.mobius.unit.dto.DeploymentWorkflowResponse;
import com.aidtaas.mobius.unit.enums.Environment;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

@Tag(name = "Camunda Controller")
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "jwt")
@Path("/v1.0/camunda")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface CamundaController {

  @SecurityRequirement(name = "jwt")
  @Operation(summary = "Deploy Workflow")
  @APIResponses(
    value = {
      @APIResponse(responseCode = "200", description = "Success",
        content = {@Content(schema = @Schema(implementation = DeploymentWorkflowResponse.class))}),
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
  @Path("/deploy/{wfId}")
  @POST
  Response deployWorkflow(@PathParam("wfId") String wfId, @Context HttpHeaders httpHeaders) throws IOException;


  @SecurityRequirement(name = "jwt")
  @Operation(summary = "Execute Workflow")
  @APIResponses(
    value = {
      @APIResponse(responseCode = "200", description = "Success",
        content = {@Content(schema = @Schema())}),
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
  @POST
  @Produces(APPLICATION_JSON)
  @Path("/execute/{wfId}")
  @Consumes({MULTIPART_FORM_DATA, APPLICATION_JSON, APPLICATION_FORM_URLENCODED})
  Response executeWorkflow(@PathParam("wfId") String wfId, @QueryParam("appId") String appId,
                           @QueryParam("env") @DefaultValue("PROD") Environment env,
                           @MultipartForm MultipartFormDataInput dataInput,
                           @Context HttpHeaders httpHeaders);
}
