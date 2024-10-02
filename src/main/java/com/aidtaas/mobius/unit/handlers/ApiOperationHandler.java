
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.handlers;

import com.aidtaas.mobius.unit.aclmodels.enums.ActionLogRequestStatus;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionSource;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionType;
import com.aidtaas.mobius.unit.aclmodels.enums.NodeType;
import com.aidtaas.mobius.unit.aclmodels.enums.RequesterType;
import com.aidtaas.mobius.unit.component.ApiOperationManager;
import com.aidtaas.mobius.unit.component.RestApiOperator;
import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiOperation;
import com.aidtaas.mobius.unit.enums.ApiType;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.exception.RetryableException;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import com.aidtaas.mobius.unit.utils.BobValidator;
import com.aidtaas.mobius.unit.utils.CommonUtils;
import com.aidtaas.mobius.unit.utils.MeteringUtils;
import com.aidtaas.mobius.unit.utils.TfUtils;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION_GLOBAL;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_ID_GLOBAL;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_TYPE_GLOBAL;

/**
 * This class represents an API operation handler.
 * It is annotated with @Slf4j, @ApplicationScoped, and @RequiredArgsConstructor.
 * These annotations provide a logger, specify that the handler is application-scoped,
 * and generate a constructor that initializes the final fields.
 * The handler includes a method to execute an external task.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ApiOperationHandler implements ExternalTaskHandler, JavaDelegate {

  private final BobValidator validator;

  private final ApiOperationManager apiOperationManager;

  private final TfUtils tfUtils;

  private final RestApiOperator restApiOperator;

  private final ActionLogUtil actionLogUtil;

  private final ConfigProperties config;

  /**
   * Executes an external task.
   * It logs the start and end of the execution, validates the API operation,
   * performs the operation if it is a REST operation, handles any retryable exceptions, and completes the task.
   *
   * @param externalTask        the external task
   * @param externalTaskService the external task service
   */
  @Override
  public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {

    log.info("-------start-------Thread name and id at ApiOperationHandler, {} {}",
      Thread.currentThread().getName(), Thread.currentThread().threadId());

    long currentTimeMillisStart = System.currentTimeMillis();
    log.debug("Starting time in milliseconds : {}", currentTimeMillisStart);

    String workflowId = CommonUtils.getWorkflowId(externalTask);
    log.info("*********** BEGIN [{}] - [{}] **************", externalTask.getTopicName(), workflowId);

    var requesterType = RequesterType.valueOf(externalTask.getVariable(REQUESTER_TYPE_GLOBAL));
    var requesterId = externalTask.getVariable(REQUESTER_ID_GLOBAL).toString();

    ApiOperation apiOperation = new ApiOperation(externalTask, externalTaskService);

    try {
      validator.validateApiOperation(apiOperation);
      if (apiOperation.getInput().getApiType().getValue().equalsIgnoreCase(ApiType.REST.getValue())) {
        apiOperationManager.retrieveRestApiInfo(apiOperation);
        restApiOperator.performOperation(apiOperation, externalTask);
      } else {
        throw new NonRetryableException("Operation of this type is not yet supported! ");
      }

      long currentTimeMillisEnd = System.currentTimeMillis() - currentTimeMillisStart;

      log.info("Time taken by Api Operation Handler : {}", currentTimeMillisEnd);

      log.info("------end--------Thread name and id at ApiOperationHandler, {} {}", Thread.currentThread().getName(),
        Thread.currentThread().threadId());
      log.info("################### END [{}] - [{}] #####################", externalTask.getTopicName(),
        workflowId);

      externalTaskService.complete(externalTask, apiOperation.getRuntimeVariables());

      CompletableFuture.runAsync(() ->
        actionLogUtil.actionLog(ActionSource.SYSTEM_CONSUMER, ActionType.GET, requesterType, requesterId,
          NodeType.BRICKS, externalTask.getActivityId(), null,
          null, ActionLogRequestStatus.SUCCESS));

    } catch (RetryableException retryableException) {

      constructErrorDetails(externalTask, retryableException, apiOperation, requesterType, requesterId);

      apiOperation.getRuntimeVariables().put(String.format(BobConstants.CURRENT_RESPONSE_CODE,
        apiOperation.getInput().getActivityId()), retryableException.getStatusCode());

      int retries = CommonUtils.calculateRetries(externalTask, config.retryCount());
      long retryTimeout = (long) config.retryDelay() *(config.retryCount()-retries);

      externalTaskService.handleFailure(externalTask.getId(), "Failed to call the rest api",
        retryableException.getMessage(), retries, retryTimeout, apiOperation.getRuntimeVariables(), null);

      if(retries==0) {
        externalTaskService.handleBpmnError(externalTask, String.valueOf(retryableException.getStatusCode()),
          retryableException.getMessage(), apiOperation.getRuntimeVariables());
      }

    } catch (NonRetryableException nonRetryableException) {

      constructErrorDetails(externalTask, nonRetryableException, apiOperation, requesterType, requesterId);

      apiOperation.getRuntimeVariables().put(String.format(BobConstants.CURRENT_RESPONSE_CODE,
        apiOperation.getInput().getActivityId()), nonRetryableException.getStatusCode());

      externalTaskService.handleFailure(externalTask.getId(), BobConstants.ERROR_FAILED_TO_CALL_REST_API,
        nonRetryableException.toString(), 0, 0, apiOperation.getRuntimeVariables(), null);

      externalTaskService.handleBpmnError(externalTask, String.valueOf(nonRetryableException.getStatusCode()),
        nonRetryableException.getMessage(), apiOperation.getRuntimeVariables());

    } catch (RuntimeException e) {

      constructErrorDetails(externalTask, e, apiOperation, requesterType, requesterId);

      externalTaskService.handleFailure(externalTask.getId(), BobConstants.ERROR_FAILED_TO_CALL_REST_API,
        e.toString(), 0, 0, apiOperation.getRuntimeVariables(), null);

      externalTaskService.handleBpmnError(externalTask, "error", e.getMessage(),
        apiOperation.getRuntimeVariables());

    }

  }

  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {

  }

  private void constructErrorDetails(ExternalTask externalTask, Exception apiException, ApiOperation apiOperation, RequesterType requesterType, String requesterId) {

    log.error("Failed to call rest api || {}", apiException.getMessage(), apiException);

    apiOperation.getRuntimeVariables().put(String.format(BobConstants.GLOBAL_ERROR_VARIABLE,
      apiOperation.getInput().getComponentName(), externalTask.getActivityId()), apiException.getMessage());

    var jobStatusDTO = MeteringUtils.newMeteringJobStat(externalTask);
    jobStatusDTO.setState("ERROR");
    jobStatusDTO.setMessage(apiException.getMessage());

    CompletableFuture.runAsync(() -> tfUtils.sendToTF(jobStatusDTO,
      externalTask.getVariable(AUTHORIZATION_GLOBAL).toString(), config.jobStatusDtoSchemaId()));

    CompletableFuture.runAsync(() ->
      actionLogUtil.actionLog(ActionSource.SYSTEM_CONSUMER, ActionType.GET, requesterType, requesterId,
        NodeType.BRICKS, externalTask.getActivityId(), null,
        null, ActionLogRequestStatus.FAILED));
  }


}
