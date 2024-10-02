
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
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.dto.DeploymentResponse;
import com.aidtaas.mobius.unit.dto.DeploymentWorkflowResponse;
import com.aidtaas.mobius.unit.dto.ProcessInstanceResponse;
import com.aidtaas.mobius.unit.dto.TriggerWorkflowResponse;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.exception.ApiException;
import com.aidtaas.mobius.unit.exception.ValidationException;
import com.aidtaas.mobius.unit.model.Workflow;
import com.aidtaas.mobius.unit.service.CamundaService;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import com.aidtaas.mobius.unit.utils.CamundaUtils;
import com.aidtaas.mobius.unit.utils.URLResolver;
import com.aidtaas.mobius.unit.utils.WorkflowUtils;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.aidtaas.mobius.unit.constants.BobConstants.*;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class CamundaServiceImpl implements CamundaService {

  private final ActionLogUtil actionLogUtil;

  private final DynamicRestClient dynamicRestClient;

  private final CamundaUtils camundaUtils;

  private final WorkflowUtils workflowUtils;

  private final ConfigProperties serviceConfig;

  private final URLResolver urlResolver;

  /**
   * Deploys a workflow to the Camunda engine.
   *
   * @param wfId          The ID of the workflow to be deployed.
   * @param requesterId   The ID of the requester who is deploying the workflow.
   * @param requesterType The type of the requester (e.g., HUMAN_API, SYSTEM_API).
   * @param auth          The authorization token for the requester.
   * @return A DeploymentWorkflowResponse object containing the deployment status and any errors
   * that occurred during deployment.
   * @throws IOException If there is an issue with the API call to save the existing
   *                     workflow.
   */
  @Override
  public Response deployWorkflow(String wfId, String requesterId, RequesterType requesterType, String auth)
    throws IOException {

    Workflow workflow = workflowUtils.getLatestWorkflow(serviceConfig.serviceLatestWf() + wfId, auth);
    var response = new DeploymentWorkflowResponse();
    if (ObjectUtils.isNotEmpty(workflow.getDeployedVersion())) {
      response.setMessage(WF_ALREADY_DEPLOYED);
      return Response.status(Status.OK).entity(response).build();
    }
    var deploymentResponse = deployAndGetDeploymentResponse(workflow, wfId, requesterId);
    if (ObjectUtils.isEmpty(deploymentResponse.getDeployedProcessDefinitions())) {
      if (ObjectUtils.isNotEmpty(deploymentResponse.getId())) {
        response.setDeploymentId(deploymentResponse.getId());
        response.setMessage(WF_ALREADY_DEPLOYED);
        return Response.status(Status.OK).entity(response).build();
      } else {
        throw new ValidationException("Something is wrong in the workflow xml, please check the xml");
      }
    }
    workflow.setDeploymentId(deploymentResponse.getId());
    workflow.setStatus(DEPLOYED);

    String configUrl = serviceConfig.serviceDeployedVersion().replace(WF_ID, wfId.toString());
    Integer latestDeployedVersion = camundaUtils.getWorkflowLatestDeployedVersionApiCall(configUrl, auth);
    workflow.setDeployedVersion(latestDeployedVersion + 1);
    response.setDeploymentId(deploymentResponse.getId());
    response.setSuccess(true);
    response.setMessage("Workflow deployed Successfully");
    saveWorkflowAsDeployed(workflow, auth, requesterId);
    logWorkflowActions(workflow, requesterId, requesterType, wfId, auth, response);
    return Response.status(Status.OK).entity(response).build();
  }

  /**
   * Deploys a workflow to the Camunda engine and returns the deployment response.
   *
   * @param process  The workflow to be deployed.
   * @param id       The ID of the workflow to be deployed.
   * @param tenantId
   * @return A DeploymentResponse object containing the deployment status and any errors that
   * occurred during deployment.
   * @throws IOException If there is an issue with the API call to deploy the workflow.
   */
  private DeploymentResponse deployAndGetDeploymentResponse(Workflow process,
                                                            String id, String tenantId) throws IOException {

    String xml = process.getXml();
    xml = xml.replaceAll(BPMN_PROCESS_GAIANWF + id + "\" name=\"[^\"]*+\"",
      BPMN_PROCESS_GAIANWF + id + "\" name=\"" + process.getName() + "\"");
    return camundaUtils.deployToCamunda(xml, id, tenantId);
  }

  /**
   * Saves the workflow as deployed in the Camunda engine.
   *
   * @param process  The workflow to be saved as deployed.
   * @param auth     The authorization token for the requester.
   * @param tenantId
   * @throws ApiException If there is an issue with the API call to save the existing workflow.
   */
  private void saveWorkflowAsDeployed(Workflow process, String auth, String tenantId) throws ApiException {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(AUTHORIZATION, auth);

    Map<String, String> pathParams = new HashMap<>();
    pathParams.put("wfId", String.valueOf(process.getWfId()));
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("deployedVersion", String.valueOf(process.getDeployedVersion()));
    queryParams.put("deploymentId", process.getDeploymentId());
    String saveExistingWfUrl = urlResolver.constructUrl(serviceConfig.serviceSaveDeployment(), queryParams, pathParams);

    var saveResponse = dynamicRestClient.makeApiCall(saveExistingWfUrl, null, PUT, headers);
    if (saveResponse.getStatusCodeValue() == HTTP_STATUS_CODE_200) {
      log.info("Updated workflow saved successfully after deployment");
    }
  }

  /**
   * Logs the workflow actions in the ACL.
   *
   * @param process       The workflow for which the actions are to be logged.
   * @param requesterId   The ID of the requester who is deploying the workflow.
   * @param requesterType The type of the requester (e.g., HUMAN_API, SYSTEM_API).
   * @param id            The ID of the workflow to be deployed.
   * @param auth          The authorization token for the requester.
   */
  private void logWorkflowActions(Workflow process, String requesterId, RequesterType requesterType,
                                  String id, String auth, Object response) {
    CompletableFuture.runAsync(() ->
      actionLogUtil.createNodeRelationsForConstructs(process, requesterId, requesterType, auth));
    CompletableFuture.runAsync(() -> actionLogUtil.actionLog(ActionSource.HUMAN_API,
      ActionType.CREATE, requesterType, requesterId, NodeType.WORKFLOW, id, null, response, ActionLogRequestStatus.SUCCESS));
  }

  /**
   * Triggers a workflow in the Camunda engine.
   *
   * @param id            The ID of the workflow to be triggered.
   * @param requesterId   The ID of the requester who is triggering the workflow.
   * @param requesterType The type of the requester (e.g., HUMAN_API, SYSTEM_API).
   * @param auth          The authorization token for the requester.
   * @param env
   * @param dataInput
   * @return A TriggerWorkflowResponse object containing the status of the triggered workflow and
   * any errors that occurred during triggering.
   * @throws ValidationException    If there is an issue with the validation of the workflow or its
   *                                inputs.
   * @throws ObjectMappingException If there is an issue with mapping the inputs to the workflow
   *                                variables.
   * @throws ApiException           If there is an issue with the API call to trigger the workflow.
   */
  @Override
  public Response triggerWorkflow(String id, String requesterId, RequesterType requesterType,
                                  String auth, Environment env, String appId, MultipartFormDataInput dataInput) {

    Map<String, Object> variables = new HashMap<>();
    Map<String, InputPart> fileInputs = new HashMap<>();
    if (ObjectUtils.isNotEmpty(dataInput)) {
      camundaUtils.convertMultipartFormDataInputToMap(dataInput, variables, fileInputs);
    }

    prepareVariables(id, appId, env, auth, requesterId, requesterType, variables);
    if (MapUtils.isNotEmpty(fileInputs)) {
      camundaUtils.processFileInputs(fileInputs, variables);
    }

    ProcessInstanceResponse procInstRes = camundaUtils.startProcessInstance(id, variables,
      auth);
    var response = TriggerWorkflowResponse.builder().tenantId(requesterId).env(env).build();

    if (procInstRes.isEnded()) {
      response.setStatus(EXECUTED);
    } else {
      response.setStatus(RUNNING);
    }

    response.setProcessInstanceId(procInstRes.getId());
    response.setSuccess(true);

    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.CREATE, requesterType, requesterId,
        NodeType.WORKFLOW, id, null, response, ActionLogRequestStatus.SUCCESS));

    return Response.ok().entity(response).build();
  }

  /**
   * Prepares the variables required for triggering a workflow in the Camunda engine.
   *
   * @param env           The environment in which the workflow is to be triggered.
   * @param auth          The authorization token for the requester.
   * @param requesterId   The ID of the requester who is triggering the workflow.
   * @param requesterType The type of the requester (e.g., HUMAN_API, SYSTEM_API).
   * @return A map containing the prepared variables.
   */
  public Map<String, Object> prepareVariables(String id, String appId, Environment env, String auth,
                                              String requesterId, RequesterType requesterType,
                                              Map<String, Object> variables) {

    variables.put(WORKFLOW_ID_GLOBAL, id);
    variables.put(ENVIRONMENT_GLOBAL, env.toString());
    variables.put(AUTHORIZATION_GLOBAL, auth);

    if (Environment.PROD == env) {
      validateProdEnvironment(requesterId, appId);
      variables.put(APP_ID, appId);
      variables.put(APP_ID_GLOBAL, appId);
    }

    variables.put(TENANT_ID_GLOBAL, requesterId);
    variables.put(TENANT_ID, requesterId);

    variables.put(REQUESTER_ID_GLOBAL, requesterId);
    variables.put(REQUESTER_TYPE_GLOBAL, requesterType);

    return variables;
  }

  /**
   * Validates the production environment variables.
   *
   * @param requesterId The ID of the requester who is triggering the workflow.
   * @param appId       The application ID for the workflow.
   */
  private static void validateProdEnvironment(String requesterId, String appId) {
    if (StringUtils.isEmpty(requesterId)) {
      throw new ValidationException(
        "Executor Tenant ID is required for executing workflow in PROD environment");
    }
    if (StringUtils.isEmpty(appId)) {
      throw new ValidationException(
        "App ID is required for executing workflow in PROD environment");
    }
  }
}
