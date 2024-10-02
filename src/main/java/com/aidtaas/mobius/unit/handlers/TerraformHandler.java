
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
import com.aidtaas.mobius.unit.component.ResolveScriptData;
import com.aidtaas.mobius.unit.component.ScriptOperationManager;
import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.InfraDTO;
import com.aidtaas.mobius.unit.dto.ScriptOperation;
import com.aidtaas.mobius.unit.dto.UserDTO;
import com.aidtaas.mobius.unit.enums.ScriptSource;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import com.aidtaas.mobius.unit.utils.CommonUtils;
import com.aidtaas.mobius.unit.utils.TfUtils;
import com.aidtaas.mobius.unit.utils.BobValidator;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import static com.aidtaas.mobius.unit.constants.BobConstants.PATH_DELIMITER;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_ID_GLOBAL;
import static com.aidtaas.mobius.unit.constants.BobConstants.REQUESTER_TYPE_GLOBAL;

/**
 * This class represents a shell script handler. It is annotated with @Slf4j and @ApplicationScoped.
 * These annotations provide a logger and specify that the handler is application-scoped. The
 * handler includes a method to execute a shell script command.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TerraformHandler  implements ExternalTaskHandler, JavaDelegate {

  public static final int LOG_READER_THREAD_POOL = 2;
  private final BobValidator validator;

  private final TfUtils tfUtils;

  private final ConfigProperties config;

  private final ScriptOperationManager scriptOperationManager;

  private final ResolveScriptData resolveScriptData;

  private final ActionLogUtil actionLogUtil;


  /**
   * Executes an external task. It logs the start and end of the execution and the variables of the
   * task.
   *
   * @param externalTask        the external task
   * @param externalTaskService the external task service
   */
  @Override
  public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
    String workflowId = CommonUtils.getWorkflowId(externalTask);
    log.info("************* Starting TerraformHandler - [{}] **************", workflowId);

    ScriptOperation scriptOperation = new ScriptOperation(externalTask, externalTaskService);

    var requesterType = RequesterType.valueOf(externalTask.getVariable(REQUESTER_TYPE_GLOBAL));
    var requesterId = externalTask.getVariable(REQUESTER_ID_GLOBAL).toString();

    try {
      validator.validateScriptOperation(scriptOperation);
      scriptOperationManager.retrieveScriptInfo(scriptOperation);

      log.info("---- Preparing and executing the commands ----");

      prepareDataAndExecuteCommands(scriptOperation);

      log.info("---- Completing the external task ----");

      externalTaskService.complete(externalTask, scriptOperation.getRuntimeVariables());

      log.info("---- External task is completed ----");

      CompletableFuture.runAsync(() ->
        actionLogUtil.actionLog(ActionSource.SYSTEM_CONSUMER, ActionType.GET, requesterType, requesterId,
          NodeType.BRICKS, externalTask.getActivityId(), null,
          null, ActionLogRequestStatus.SUCCESS));

    } catch (Exception e) {
      log.error("Error while executing the terraform script", e);
      scriptOperation.getRuntimeVariables().put(String.format(BobConstants.GLOBAL_ERROR_VARIABLE,
        scriptOperation.getInput().getComponentName(), externalTask.getActivityId()), e.getMessage());

      int retries = CommonUtils.calculateRetries(externalTask, 0);
      long retryTimeout = (long) config.retryDelay() * (config.retryCount() - retries);

      externalTaskService.handleFailure(externalTask.getId(), "Failed to execute the terraform script",
        e.getMessage(), retries, retryTimeout, scriptOperation.getRuntimeVariables(), null);

      externalTaskService.handleBpmnError(externalTask, "error", e.getMessage(), scriptOperation.getRuntimeVariables());

      CompletableFuture.runAsync(() ->
        actionLogUtil.actionLog(ActionSource.SYSTEM_CONSUMER, ActionType.GET, requesterType, requesterId,
          NodeType.BRICKS, externalTask.getActivityId(), null,
          null, ActionLogRequestStatus.FAILED));
    }

    log.info("************* End TerraformHandler **************");
  }

  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {

  }

  public void prepareDataAndExecuteCommands(ScriptOperation scriptOperation) {

    Map<String, Object> inputFields = resolveScriptData.resolveScriptVariables(scriptOperation);

    CommonUtils.ScriptInputData scriptData = CommonUtils.getScriptInputData(scriptOperation, inputFields);

    JSch jsch = CommonUtils.prepareJSch(scriptData.privateKeyPath(), scriptData.password());

    Session session = CommonUtils.getSession(scriptData.user(), scriptData.privateKeyPath(), scriptData.host(),
      scriptData.port(), scriptData.password(), jsch);

    String terraformPath = String.format(BobConstants.TERRAFORM_PATH_TEMPLATE, scriptOperation.getExecutorTenantId(),
      scriptOperation.getExternalTask().getActivityId(), scriptData.staticPath());
    String folderName = BobConstants.TERRAFORM_FOLDER_PREFIX + Instant.now().toEpochMilli();
    String remoteFile = folderName + scriptData.scriptPath() + PATH_DELIMITER + BobConstants.TERRAFORM_TFVARS;
    List<String> data;

    try {

      List<List<String>> commands = prepareCommands(terraformPath, scriptData, folderName);

      createTerraformVariablesFile(scriptData.finalScriptVariables(), BobConstants.TERRAFORM_TFVARS);

      executeAllCommands(scriptOperation, terraformPath, session, commands, BobConstants.TERRAFORM_TFVARS, remoteFile);

      data = new ArrayList<>();
      CommonUtils.executeSingleCommand(scriptOperation, terraformPath, session,
        String.format(BobConstants.TERRAFORM_SHOW_JSON, folderName, scriptData.scriptPath()), data);

    } finally {
      cleanup(scriptOperation, session, terraformPath, folderName);
    }

    if (CollectionUtils.isNotEmpty(data)) {
      processTerraformOutput(data, scriptOperation);
    }
  }

  private List<List<String>> prepareCommands(String terraformPath, CommonUtils.ScriptInputData scriptData, String folderName) {

    List<List<String>> commands = new ArrayList<>();
    commands.add(new ArrayList<>());

    commands.get(0).add(String.format(BobConstants.MKDIR_COMMAND, folderName));

    if (ScriptSource.GIT.getValue().equalsIgnoreCase(scriptData.scriptSource().getValue())) {
      commands.get(0).add(String.format(BobConstants.GIT_CLONE_COMMAND, folderName, scriptData.file()));
    } else {
      throw new NonRetryableException(String.format(BobConstants.SCRIPT_SOURCE_NOT_SUPPORTED_YET, scriptData.scriptSource().getValue()));
    }

    String currentPath = folderName + scriptData.scriptPath();

    //TODO: Should come from vault secrets
    String storageAccountKey = "9GejUaGBY9Ce56nyDPHMXj4vZDvzetI8nuVxMMUM3VlVZzLdgTzb4B1M9sGdFvHv6ptoR8JnzX\\/K+AStvDxO4Q==";

    commands.get(0).add(String.format(BobConstants.TERRAFORM_SED_COMMAND, currentPath, terraformPath, storageAccountKey));

    commands.add(new ArrayList<>());
    commands.get(1).add(String.format(BobConstants.TERRAFORM_INIT_COMMAND, currentPath));
    commands.get(1).add(String.format(BobConstants.TERRAFORM_PLAN_COMMAND, currentPath));

    if (scriptData.create()) {
      log.info("---- Creating Infra ----");
      commands.get(1).add(String.format(BobConstants.TERRAFORM_APPLY_COMMAND, currentPath));
    } else {
      log.info("---- Destroying Infra ----");
      commands.get(1).add(String.format(BobConstants.TERRAFORM_DESTROY_COMMAND, currentPath));
    }

    return commands;
  }

  private void createTerraformVariablesFile(Map<String, Object> finalScriptVariables, String fileName) {
    try (FileWriter fileWriter = new FileWriter(fileName)) {
      for (Map.Entry<String, Object> entry : finalScriptVariables.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        String valueString = convertToTerraformFormat(value);
        fileWriter.write(key + " = " + valueString + "\n");
      }
    } catch (IOException e) {
      log.error("Error while writing to the file", e);
    }
  }

  /**
   * Gets the strings. It logs the session and commands.
   *
   * @param session  the session
   * @param commands the commands
   * @return the list of strings
   */
  private static void executeAllCommands(ScriptOperation scriptOperation, String terraformPath, Session session,
                                         List<List<String>> commands, String fileName, String remoteFile) {
    ArrayList<String> result = new ArrayList<>();
    IntStream.range(0, commands.size()).forEach((int index) ->
      executeCommandsAndReplaceFile(scriptOperation, terraformPath, session, commands, fileName, remoteFile, index, result));
  }

  private static void executeCommandsAndReplaceFile(ScriptOperation scriptOperation, String terraformPath, Session session,
                                                    List<List<String>> commands, String fileName, String remoteFile,
                                                    int index, ArrayList<String> result) {
    commands.get(index).forEach((String command) ->
      CommonUtils.executeSingleCommand(scriptOperation, terraformPath, session, command, result));
    if (index < 1) {
      replaceFile(session, fileName, remoteFile);
    }
  }

  private void cleanup(ScriptOperation scriptOperation, Session session, String terraformPath, String folderName) {
    if (StringUtils.isNotEmpty(terraformPath)) {
      log.info("---- Cleaning up the files ----");
      CommonUtils.executeSingleCommand(scriptOperation, terraformPath, session, "rm -rf " + folderName, new ArrayList<>());
    }
    log.info("-------------Disconnecting session-------------");
    session.disconnect();
  }

  private void processTerraformOutput(List<String> data, ScriptOperation scriptOperation) {
    StringBuilder jsonOutput = new StringBuilder();
    data.forEach(jsonOutput::append);
    String jsonString = jsonOutput.toString();
    resolveScriptData.processOutput(jsonString, scriptOperation);
    sendDataToPi(jsonString, scriptOperation);
    log.info("Data Execution Result Completed");
  }

  private void sendDataToPi(String outputData, ScriptOperation scriptOperation) {
    log.info("Sending Infra Data to PI");
    String authorization = scriptOperation.getExternalTask().getVariable(BobConstants.AUTHORIZATION_GLOBAL).toString();
    String primaryKey = scriptOperation.getExternalTask().getActivityId() + "_" + scriptOperation.getExternalTask().getActivityInstanceId();
    UserDTO userDTO = UserDTO.builder().
      id(primaryKey).
      tenantId(scriptOperation.getExecutorTenantId()).build();
    CompletableFuture.runAsync(() -> tfUtils.sendToTF(userDTO, authorization, config.userDtoSchemaId()));
    InfraDTO infraDTO;
    try {
      infraDTO = Config.OBJECT_MAPPER.readValue(outputData, InfraDTO.class);
      infraDTO.setId(primaryKey);
    } catch (JsonProcessingException e) {
      log.error("Error while parsing the infra data", e);
      throw new ObjectMappingException("Error parsing infra Schema", e);
    }
    CompletableFuture.runAsync(() -> tfUtils.sendToTF(infraDTO, authorization, config.infraDtoSchemaId()));
  }

  private static void replaceFile(Session session, String localFile, String remoteFile) {
    Channel channel = null;
    ChannelSftp channelSftp = null;

    try {
      // Avoid asking for key confirmation
      Properties config = new Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);

      // Open SFTP channel
      channel = session.openChannel("sftp");
      channel.connect();

      channelSftp = (ChannelSftp) channel;

      // Use try-with-resources for FileInputStream
      try (FileInputStream fis = new FileInputStream(localFile)) {
        channelSftp.put(fis, remoteFile, ChannelSftp.OVERWRITE);
      }

      log.info("File copied and overwritten successfully on remote server.");
    } catch (Exception e) {
      log.error("Error while replacing the file", e);
    } finally {
      if (channelSftp != null) {
        channelSftp.disconnect();
      }
      if (channel != null) {
        channel.disconnect();
      }
    }
  }

  private static String convertToTerraformFormat(Object value) {
    String convertedValue;
    if (value instanceof String strValue) {
      convertedValue = convertString(strValue);
    } else if (value instanceof Boolean || value instanceof Number) {
      convertedValue = value.toString();
    } else if (value instanceof List<?> list) {
      convertedValue = list.stream()
        .map(TerraformHandler::convertToTerraformFormat)
        .collect(Collectors.joining(", ", "[", "]"));
    } else if (value instanceof Map<?, ?> map) {
      convertedValue = map.entrySet().stream()
        .map(entry -> "  " + entry.getKey() + " = " + convertToTerraformFormat(entry.getValue()))
        .collect(Collectors.joining("\n", "{\n", "\n}"));
    } else {
      try {
        convertedValue = Config.OBJECT_MAPPER.writeValueAsString(value);
      } catch (JsonProcessingException e) {
        log.error("Error while parsing the script variable", e);
        throw new ObjectMappingException("Error parsing script variable", e);
      }
    }
    return convertedValue;
  }

  private static String convertString(String value) {
    if (value.startsWith("[") && value.endsWith("]")) {
      return value;
    } else if (value.startsWith("{") && value.endsWith("}")) {
      return value;
    } else {
      return "\"" + value + "\"";
    }
  }



}
