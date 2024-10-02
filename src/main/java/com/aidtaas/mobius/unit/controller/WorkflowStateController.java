
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller;

import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.util.concurrent.CompletionStage;


@Path("/v1.0/wf")
public interface WorkflowStateController {

  @Operation(summary = "Suspend running jobs for given tenantId", hidden = true)
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
  @PUT
  @Path("/running-jobs/suspend")
  CompletionStage<Response> suspendRunningJobsByTenantId(@Context HttpHeaders httpHeaders);


  @Operation(summary = "Activate running jobs for given tenantId", hidden = true)
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
  @PUT
  @Path("/running-jobs/activate")
  CompletionStage<Response> activateRunningJobsByTenantId(@Context HttpHeaders httpHeaders);


  @Operation(summary = "Suspend workflow instance for given tenantId", hidden = true)
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
  @PUT
  @Path("/workflow-instances/suspend")
  CompletionStage<Response> suspendWorkflowInstanceByTenantId(@Context HttpHeaders httpHeaders);


  @Operation(summary = "Activate workflow instance for given tenantId", hidden = true)
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
  @PUT
  @Path("/workflow-instances/activate")
  CompletionStage<Response> activateWorkflowInstanceByTenantId(@Context HttpHeaders httpHeaders);


  @Operation(summary = "Suspend workflows for given tenantId", hidden = true)
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
  @PUT
  @Path("/suspend")
  CompletionStage<Response> suspendWorkflowsByTenantId(@Context HttpHeaders httpHeaders);


  @Operation(summary = "Activate workflows for given tenantId", hidden = true)
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
  @PUT
  @Path("/activate")
  CompletionStage<Response> activateWorkflowsByTenantId(@Context HttpHeaders httpHeaders);
}
