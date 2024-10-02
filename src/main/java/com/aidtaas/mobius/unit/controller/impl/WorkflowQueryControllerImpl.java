
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller.impl;

import com.aidtaas.mobius.unit.aclmodels.enums.ActionLogRequestStatus;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionSource;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionType;
import com.aidtaas.mobius.unit.aclmodels.enums.NodeType;
import com.aidtaas.mobius.unit.aclmodels.enums.RequesterType;
import com.aidtaas.mobius.unit.controller.WorkflowQueryController;
import com.aidtaas.mobius.unit.exception.ApiException;
import com.aidtaas.mobius.unit.repositories.RunningWorkflowInstanceRepo;
import com.aidtaas.mobius.unit.service.WorkflowQueryService;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_ID;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_TYPE;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class WorkflowQueryControllerImpl implements WorkflowQueryController {

  private final WorkflowQueryService workflowQueryService;
  private final RunningWorkflowInstanceRepo runningWorkflowInstanceRepo;
  private final ActionLogUtil actionLogUtil;

  /**
   * Gets the process definition count for each workflow.
   *
   * @param wfIds The workflow ids.
   * @return The response.
   */
  @Override
  public Response getSuspendedProcessDefinitionCountForEachWfs(List<String> wfIds, HttpHeaders httpHeaders) {
    try {
      log.info("POST:/v1.0/wf/process-definition/suspended/filter, wfIds:{}", wfIds);
      Map<String, String> result = runningWorkflowInstanceRepo.getSuspendedProcessDefinitionCountForEachWfs(wfIds);
      CompletableFuture.runAsync(() -> actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.GET,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.FAILED));
      return Response.ok(result).build();
    } catch (ApiException e) {
      CompletableFuture.runAsync(() -> actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.GET,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, null, null, null, ActionLogRequestStatus.FAILED));
      throw e;
    }
  }

  /**
   * Gets the suspended process definition count.
   *
   * @param wfId            The workflow id.
   * @param deployedVersion The deployed version.
   * @return The response.
   */
  @Override
  public Response getSuspendedProcDefCount(String wfId, Integer deployedVersion, HttpHeaders httpHeaders) {
    try {
      log.info("GET:/v1.0/wf/process-definition/suspended, wfId:{} and deployedVersion:{}", wfId, deployedVersion);
      Long result = workflowQueryService.getSuspendedProcDefCount(wfId, deployedVersion, httpHeaders);
      return Response.ok(result).build();
    } catch (ApiException e) {
      CompletableFuture.runAsync(() -> actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.GET,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, wfId, null, null, ActionLogRequestStatus.FAILED));
      throw e;
    }
  }

  /**
   * Gets the activity data count.
   *
   * @param wfId              The workflow id.
   * @param processInstanceId The process instance id.
   * @param httpHeaders       The http headers.
   * @return The response.
   */
  @Override
  public Response getActivityDataCount(String wfId, String processInstanceId, HttpHeaders httpHeaders) {
    try {
      log.info("GET:/v1.0/wf/activity/data/count processInstanceId:{}", processInstanceId);
      var auth = httpHeaders.getHeaderString(AUTHORIZATION);

      Map<String, Long> activityDataCount = workflowQueryService.
        getActivityDataCount(wfId, processInstanceId, auth, httpHeaders);
      return Response.ok(activityDataCount).build();
    } catch (ApiException e) {
      CompletableFuture.runAsync(() -> actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.GET,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, wfId, null, null, ActionLogRequestStatus.FAILED));
      throw e;
    }
  }
}
