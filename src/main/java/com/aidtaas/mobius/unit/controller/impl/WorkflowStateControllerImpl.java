
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller.impl;

import com.aidtaas.mobius.unit.controller.WorkflowStateController;
import com.aidtaas.mobius.unit.service.WorkflowStateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class WorkflowStateControllerImpl implements WorkflowStateController {

  private final WorkflowStateService workflowStateService;

  /**
   * Suspend running jobs by tenant id.
   *
   * @param httpHeaders The http headers.
   * @return The response.
   */
  @Override
  public CompletionStage<Response> suspendRunningJobsByTenantId(@Context HttpHeaders httpHeaders) {
    log.info("PUT:/v1.0/wf/running-jobs/suspend");
    return workflowStateService.suspendRunningJobsByTenantId(httpHeaders);
  }

  /**
   * Activate running jobs by tenant id.
   *
   * @param httpHeaders The http headers.
   * @return The response.
   */
  @Override
  public CompletionStage<Response> activateRunningJobsByTenantId(@Context HttpHeaders httpHeaders) {
    log.info("PUT:/v1.0/wf/running-jobs/activate");
    return workflowStateService.activateRunningJobsByTenantId(httpHeaders);
  }

  /**
   * Suspend workflow instance by tenant id.
   *
   * @param httpHeaders The http headers.
   * @return The response.
   */
  @Override
  public CompletionStage<Response> suspendWorkflowInstanceByTenantId(@Context HttpHeaders httpHeaders) {
    log.info("PUT:/v1.0/wf/workflow-instances/suspend");
    return workflowStateService.suspendWorkflowInstanceByTenantId(httpHeaders);
  }

  /**
   * Activate workflow instance by tenant id.
   *
   * @param httpHeaders The http headers.
   * @return The response.
   */
  @Override
  public CompletionStage<Response> activateWorkflowInstanceByTenantId(@Context HttpHeaders httpHeaders) {
    log.info("PUT:/v1.0/wf/workflow-instances/activate");
    return workflowStateService.activateWorkflowInstanceByTenantId(httpHeaders);
  }

  /**
   * Suspend workflows by tenant id.
   *
   * @param httpHeaders The http headers.
   * @return The response.
   */
  @Override
  public CompletionStage<Response> suspendWorkflowsByTenantId(@Context HttpHeaders httpHeaders) {
    log.info("PUT:/v1.0/wf/suspend");
    return workflowStateService.suspendWorkflowsByTenantId(httpHeaders);
  }

  /**
   * Activate workflows by tenant id.
   *
   * @param httpHeaders The http headers.
   * @return The response.
   */
  @Override
  public CompletionStage<Response> activateWorkflowsByTenantId(@Context HttpHeaders httpHeaders) {
    log.info("PUT:/v1.0/wf/activate");
    return workflowStateService.activateWorkflowsByTenantId(httpHeaders);
  }
}
