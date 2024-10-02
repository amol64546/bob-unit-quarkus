
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
import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.repositories.RunningWorkflowInstanceRepo;
import com.aidtaas.mobius.unit.service.WorkflowQueryService;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import com.aidtaas.mobius.unit.utils.WorkflowUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.aidtaas.mobius.unit.constants.BobConstants.GAIANWORKFLOWS;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_ID;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_TYPE;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class WorkflowQueryServiceImpl implements WorkflowQueryService {

  private final WorkflowUtils workflowUtils;

  private final ActionLogUtil actionLogUtil;

  private final ConfigProperties serviceConfig;

  private final RunningWorkflowInstanceRepo runningWorkflowInstanceRepo;

  /**
   * Gets suspended proc def.
   *
   * @param wfId            the wf id
   * @param deployedVersion the deployed version
   * @return the suspended proc def count
   */
  @Override
  public Long getSuspendedProcDefCount(String wfId, Integer deployedVersion, HttpHeaders httpHeaders) {
    String procDefId = GAIANWORKFLOWS + wfId + ":" + deployedVersion;
    Long suspendedProcDefCount = runningWorkflowInstanceRepo.getSuspendedProcDef(procDefId);
    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.GET,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, String.valueOf(wfId), null, null, ActionLogRequestStatus.SUCCESS));
    return suspendedProcDefCount;
  }

  /**
   * Gets activity data count.
   *
   * @param wfId              the wf id
   * @param processInstanceId the process instance id
   * @param auth              the auth
   * @param httpHeaders       the http headers
   * @return the activity data count
   */
  @Override
  public Map<String, Long> getActivityDataCount(String wfId, String processInstanceId,
                                                String auth, HttpHeaders httpHeaders) {

    String configUrl = serviceConfig.serviceLatestWf() + wfId;
    var workflow = workflowUtils.getLatestWorkflow(configUrl, auth);

    Map<String, String> outputVariablesMap = workflowUtils.parseXMLAndMapData(workflow.getXml());

    Map<String, Long> dataSizeFromVariableMap = workflowUtils.
      getDataSizeFromVariableMap(outputVariablesMap, processInstanceId);

    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.GET,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, String.valueOf(wfId), null, null, ActionLogRequestStatus.SUCCESS));

    return dataSizeFromVariableMap;
  }
}
