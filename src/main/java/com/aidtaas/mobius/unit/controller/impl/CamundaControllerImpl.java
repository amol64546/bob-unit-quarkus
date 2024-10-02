
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
import com.aidtaas.mobius.unit.controller.CamundaController;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.exception.ApiException;
import com.aidtaas.mobius.unit.service.CamundaService;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_ID;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_TYPE;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CamundaControllerImpl implements CamundaController {

  private final CamundaService camundaService;

  private final ActionLogUtil actionLogUtil;

  /**
   * Deploys the workflow to Camunda.
   *
   * @param wfId        The workflow id.
   * @param httpHeaders The http headers.
   * @return The response.
   * @throws IOException If an error occurs.
   */
  @Override
  public Response deployWorkflow(String wfId, HttpHeaders httpHeaders) throws IOException {
    try {
      log.info("POST:/v1.0/camunda/deploy/{}", wfId);
      return camundaService.deployWorkflow(wfId, httpHeaders.getHeaderString(REQUESTER_ID),
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(AUTHORIZATION));
    } catch (ApiException e) {
      CompletableFuture.runAsync(() -> actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.CREATE,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, wfId, null, null, ActionLogRequestStatus.FAILED));
      throw e;
    }
  }

  /**
   * Executes the workflow.
   *
   * @param wfId        The workflow id.
   * @param appId       The application id.
   * @param env         The environment.
   * @param dataInput   The data input.
   * @param httpHeaders The http headers.
   * @return The response.
   */
  @Override
  public Response executeWorkflow(String wfId, String appId, Environment env, MultipartFormDataInput dataInput,
                                  HttpHeaders httpHeaders) {
    try {
      log.info("POST:/v1.0/camunda/execute/{}?env={}", wfId, env);
      return camundaService.triggerWorkflow(wfId, httpHeaders.getHeaderString(REQUESTER_ID),
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)),
        httpHeaders.getHeaderString(AUTHORIZATION), env, appId, dataInput);
    } catch (ApiException e) {
      CompletableFuture.runAsync(() -> actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.CREATE,
        RequesterType.valueOf(httpHeaders.getHeaderString(REQUESTER_TYPE)), httpHeaders.getHeaderString(REQUESTER_ID),
        NodeType.WORKFLOW, wfId, null, null, ActionLogRequestStatus.FAILED));
      throw e;
    }
  }
}
