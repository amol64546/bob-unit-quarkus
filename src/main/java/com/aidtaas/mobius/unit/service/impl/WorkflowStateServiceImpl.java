
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.service.impl;

import com.aidtaas.mobius.unit.aclmodels.enums.ActionLogRequestStatus;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionSource;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionType;
import com.aidtaas.mobius.unit.aclmodels.enums.NodeType;
import com.aidtaas.mobius.unit.aclmodels.enums.RequesterType;
import com.aidtaas.mobius.unit.repositories.RunningWorkflowInstanceRepo;
import com.aidtaas.mobius.unit.repositories.RuntimeJobRepo;
import com.aidtaas.mobius.unit.repositories.WorkflowDefinitionRepo;
import com.aidtaas.mobius.unit.service.WorkflowStateService;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_ID;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_TYPE;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class WorkflowStateServiceImpl implements WorkflowStateService {

  private final RuntimeJobRepo runtimeJobRepo;
  private final RunningWorkflowInstanceRepo runningWorkflowInstanceRepo;
  private final WorkflowDefinitionRepo workflowDefinitionRepo;
  private final ActionLogUtil actionLogUtil;

  /**
   * Suspend running jobs by tenant id.
   *
   * @param httpHeaders the http headers
   * @return the response
   */
  @Override
  public CompletionStage<Response> suspendRunningJobsByTenantId(HttpHeaders httpHeaders) {
    var requesterId = httpHeaders.getHeaderString(REQUESTER_ID);
    var requesterType = RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE));
    return CompletableFuture.supplyAsync(() -> suspendJobs(requesterId, requesterType));
  }


  private Response suspendJobs(String requesterId, RequesterType requesterType) {
    runtimeJobRepo.suspendRunningJobsByTenantId(requesterId);
    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.UPDATE, requesterType, requesterId,
        NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.SUCCESS));
    return Response.ok("Jobs suspended").build();
  }

  /**
   * Activate running jobs by tenant id.
   *
   * @param httpHeaders the http headers
   * @return the response
   */
  @Override
  public CompletionStage<Response> activateRunningJobsByTenantId(HttpHeaders httpHeaders) {
    var requesterId = httpHeaders.getHeaderString(REQUESTER_ID);
    var requesterType = RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE));
    return CompletableFuture.supplyAsync(() -> activateJobs(requesterId, requesterType));
  }

  private Response activateJobs(String requesterId, RequesterType requesterType) {
    runtimeJobRepo.activateRunningJobsByTenantId(requesterId);
    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.UPDATE, requesterType,
        requesterId, NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.SUCCESS));
    return Response.ok("Jobs activated").build();
  }

  /**
   * Suspend workflow instance by tenant id.
   *
   * @param httpHeaders the http headers
   * @return the response
   */
  @Override
  public CompletionStage<Response> suspendWorkflowInstanceByTenantId(HttpHeaders httpHeaders) {
    var requesterId = httpHeaders.getHeaderString(REQUESTER_ID);
    var requesterType = RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE));
    return CompletableFuture.supplyAsync(() -> suspendWorkflowInstances(requesterId, requesterType));
  }

  private Response suspendWorkflowInstances(String requesterId, RequesterType requesterType) {
    runningWorkflowInstanceRepo.suspendWorkflowInstanceByTenantId(requesterId);
    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.UPDATE, requesterType, requesterId,
        NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.SUCCESS));
    return Response.ok("Workflow instances suspended").build();
  }

  /**
   * Activate workflow instance by tenant id.
   *
   * @param httpHeaders the http headers
   * @return the response
   */
  @Override
  public CompletionStage<Response> activateWorkflowInstanceByTenantId(HttpHeaders httpHeaders) {
    var requesterId = httpHeaders.getHeaderString(REQUESTER_ID);
    var requesterType = RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE));
    return CompletableFuture.supplyAsync(() -> activateWorkflowInstances(requesterId, requesterType));
  }

  private Response activateWorkflowInstances(String requesterId, RequesterType requesterType) {
    runningWorkflowInstanceRepo.activateWorkflowInstanceByTenantId(requesterId);
    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.UPDATE, requesterType, requesterId,
        NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.SUCCESS));
    return Response.ok("Workflow instances activated").build();
  }

  /**
   * Suspend workflows by tenant id.
   *
   * @param httpHeaders the http headers
   * @return the response
   */
  @Override
  public CompletionStage<Response> suspendWorkflowsByTenantId(HttpHeaders httpHeaders) {
    var requesterId = httpHeaders.getHeaderString(REQUESTER_ID);
    var requesterType = RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE));
    return CompletableFuture.supplyAsync(() -> suspendWorkflows(requesterId, requesterType));
  }

  private Response suspendWorkflows(String requesterId, RequesterType requesterType) {
    workflowDefinitionRepo.suspendWorkflowsByTenantId(requesterId);
    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.UPDATE, requesterType, requesterId,
        NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.SUCCESS));
    return Response.ok("Workflows suspended").build();
  }

  /**
   * Activate workflows by tenant id.
   *
   * @param httpHeaders the http headers
   * @return the response
   */
  @Override
  public CompletionStage<Response> activateWorkflowsByTenantId(HttpHeaders httpHeaders) {
    var requesterId = httpHeaders.getHeaderString(REQUESTER_ID);
    var requesterType = RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE));
    return CompletableFuture.supplyAsync(() -> activateWorkflows(requesterId, requesterType));
  }

  private Response activateWorkflows(String requesterId, RequesterType requesterType) {
    workflowDefinitionRepo.activateWorkflowsByTenantId(requesterId);
    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.UPDATE, requesterType, requesterId,
        NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.SUCCESS));
    return Response.ok("Workflows activated").build();
  }
}
