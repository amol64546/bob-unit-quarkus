
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
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.config.URLResolver;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.dto.PipelineDto;
import com.aidtaas.mobius.unit.dto.ScriptOperation;
import com.aidtaas.mobius.unit.enums.ScriptSource;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.utils.ActionLogUtil;
import com.aidtaas.mobius.unit.utils.CommonUtils;
import com.aidtaas.mobius.unit.utils.BobValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;
import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION_GLOBAL;
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
public class ShellScriptHandler  implements ExternalTaskHandler, JavaDelegate {

  private final BobValidator validator;

  private final ScriptOperationManager scriptOperationManager;

  private final ResolveScriptData resolveScriptData;

  private final ActionLogUtil actionLogUtil;

  private final DynamicRestClient dynamicRestClient;

  private final URLResolver urlResolver;

  private final ConfigProperties config;

  /**
   * Executes an external task. It logs the start and end of the execution and the variables of the
   * task.
   *
   * @param externalTask        the external task
   * @param externalTaskService the external task service
   */
  @Override
  public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
    log.info("************* ShellScriptHandler **************");

    ScriptOperation scriptOperation = new ScriptOperation(externalTask, externalTaskService);

    var requesterType = RequesterType.valueOf(externalTask.getVariable(REQUESTER_TYPE_GLOBAL));
    var requesterId = externalTask.getVariable(REQUESTER_ID_GLOBAL).toString();

    try {
      validator.validateScriptOperation(scriptOperation);
      scriptOperationManager.retrieveScriptInfo(scriptOperation);

      prepareDataAndExecuteCommands(scriptOperation);
      externalTaskService.complete(externalTask, scriptOperation.getRuntimeVariables());
      CompletableFuture.runAsync(() ->
        actionLogUtil.actionLog(ActionSource.SYSTEM_CONSUMER, ActionType.GET, requesterType, requesterId,
          NodeType.BRICKS, externalTask.getActivityId(), null,
          null, ActionLogRequestStatus.SUCCESS));

    } catch (Exception e) {
      log.error("Error while executing the shell script", e);
      scriptOperation.getRuntimeVariables().put(String.format(BobConstants.GLOBAL_ERROR_VARIABLE,
        scriptOperation.getInput().getComponentName(), externalTask.getActivityId()), e.getMessage());

      int retries = CommonUtils.calculateRetries(externalTask, 0);
      long retryTimeout = (long) config.retryDelay() * (config.retryCount() - retries);

      externalTaskService.handleFailure(externalTask.getId(), "Failed to execute the shell script",
        e.getMessage(), retries, retryTimeout, scriptOperation.getRuntimeVariables(), null);

      externalTaskService.handleBpmnError(externalTask, "error", e.getMessage(), scriptOperation.getRuntimeVariables());

      CompletableFuture.runAsync(() ->
        actionLogUtil.actionLog(ActionSource.SYSTEM_CONSUMER, ActionType.GET, requesterType, requesterId,
          NodeType.BRICKS, externalTask.getActivityId(), null,
          null, ActionLogRequestStatus.FAILED));
    }

    log.info("************* End ShellScriptHandler **************");
  }

  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {

  }

  /**
   * Executes a shell script command. It logs the user, private key path, host, port, script, and
   * password.
   *
   * @param scriptOperation the scriptOperation
   * @throws Exception if an error occurs while executing the command
   */
  public void prepareDataAndExecuteCommands(ScriptOperation scriptOperation) throws Exception {

    Map<String, Object> inputFields = resolveScriptData.resolveScriptVariables(scriptOperation);

    CommonUtils.ScriptInputData scriptData = CommonUtils.getScriptInputData(scriptOperation, inputFields);

    JSch jsch = CommonUtils.prepareJSch(scriptData.privateKeyPath(), scriptData.password());

    Session session = CommonUtils.getSession(scriptData.user(), scriptData.privateKeyPath(), scriptData.host(),
      scriptData.port(), scriptData.password(), jsch);

    List<String> commands = prepareCommands(scriptOperation, session, scriptData, scriptData.finalScriptVariables());

    boolean async = Optional.ofNullable(scriptData.finalScriptVariables().get("async"))
      .map(Object::toString)
      .map(Boolean::parseBoolean)
      .orElse(false);

    List<String> result = new ArrayList<>();
    if (async) {
      CompletableFuture.runAsync(() -> {
        try {
          executeCommands(scriptOperation, session, commands, result);
          log.info("Async execution result: {}", result);
        } catch (Exception e) {
          log.error("Error during async execution", e);
        } finally {
          session.disconnect();
        }
      });
    } else {
      executeCommands(scriptOperation, session, commands, result);
      session.disconnect();
    }

    resolveScriptData.processOutput(Config.OBJECT_MAPPER.writeValueAsString(result), scriptOperation);
  }

  /**
   * Prepares commands. It creates a list of commands, including the script, folder name, and file
   * name.
   *
   * @param scriptInputData the script
   * @param scriptVariables the script variables
   * @return the list of commands
   */
  private List<String> prepareCommands(ScriptOperation scriptOperation, Session session, CommonUtils.ScriptInputData scriptInputData,
                                       Map<String, Object> scriptVariables) throws Exception {
    List<String> commands = new ArrayList<>();

    String folderName = BobConstants.SHELL_FOLDER_PREFIX + Instant.now().toEpochMilli();
    String finalPath = folderName + scriptInputData.scriptPath();

    if (ScriptSource.GIT.getValue().equalsIgnoreCase(scriptInputData.scriptSource().getValue())) {
      commands.add(String.format(BobConstants.MKDIR_COMMAND, folderName));
      commands.add(String.format(BobConstants.GIT_CLONE_COMMAND, folderName, scriptInputData.file()));
    } else if (ScriptSource.PIPELINE.getValue().equalsIgnoreCase(scriptInputData.scriptSource().getValue())) {
      CommonUtils.executeSingleCommand(scriptOperation, null, session, String.format(BobConstants.MKDIR_COMMAND, folderName), new ArrayList<>());
      finalPath = folderName;
      executePipeline(scriptOperation, session, finalPath, scriptInputData.file(), scriptVariables);
    } else {
      throw new NonRetryableException(String.format(BobConstants.SCRIPT_SOURCE_NOT_SUPPORTED_YET, scriptInputData.scriptSource().getValue()));
    }

    if (scriptVariables.containsKey("jmxScriptName")) {
      String jmxScriptName = scriptVariables.get("jmxScriptName").toString();
      String id = scriptVariables.get("id").toString();

      String command = String.format(BobConstants.JMETER_EXECUTE_COMMAND, jmxScriptName, id);

      commands.add(String.format(BobConstants.CD_AND_EXECUTE_COMMAND, finalPath, command));
    } else {
      String fileName = scriptInputData.scriptName();

      String finalPathToExecute = finalPath;
      List<String> sedCommands = scriptVariables.entrySet()
        .stream()
        .map(entry -> String.format(BobConstants.SED_COMMAND_FOR_PLACEHOLDER, entry.getKey(), entry.getValue(), fileName))
        .map(sedCommand -> String.format(BobConstants.CD_AND_EXECUTE_COMMAND, finalPathToExecute, sedCommand))
        .toList();

      commands.addAll(sedCommands);


      commands.add(String.format(BobConstants.CD_AND_CHMOD, finalPath, fileName));
      commands.add(String.format(BobConstants.CD_AND_EXECUTE_SCRIPT, finalPath, fileName));
    }
    return commands;
  }

  private void executePipeline(ScriptOperation scriptOperation, Session session, String finalPath, String script,
                               Map<String, Object> scriptVariables) throws Exception {
    String[] pipelineDetails = script.split("-");
    String pipelineId = pipelineDetails[0];
    String pipelineVersion = "1";
    if (pipelineDetails.length > 1) {
      pipelineVersion = pipelineDetails[1];
    }
    Map<String, String> headers = new HashMap<>();
    headers.put(AUTHORIZATION, scriptOperation.getExternalTask().getVariable(AUTHORIZATION_GLOBAL));
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("pipelineId", pipelineId);
    queryParams.put("version", pipelineVersion);
    String finalUrl = urlResolver.constructUrl(config.bobGetPipelineUrl(), queryParams, null);
    ApiResponseBody responseBody = dynamicRestClient.makeApiCall(finalUrl, null, "GET", headers);
    JsonNode jsonNode = Config.OBJECT_MAPPER.readTree(responseBody.getBody());
    PipelineDto pipelineDto = Config.OBJECT_MAPPER.convertValue(jsonNode, PipelineDto.class);
    String combinedScript = pipelineDto.getCombinedScript();
    uploadScriptViaSftp(session, finalPath, combinedScript, scriptVariables.get("fileName").toString());
  }

  private void uploadScriptViaSftp(Session session, String remoteScriptPath, String combinedContent, String fileName) throws Exception {
    ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
    sftpChannel.connect();

    String tempDirPath = System.getProperty("java.io.tmpdir");

    // Create a temporary directory to hold the temporary file with restricted permissions
    Path tempDir = Files.createTempDirectory(Paths.get(tempDirPath), "tempScripts_");

    // Create the file with the desired name in the temporary directory
    Path tempFilePath = tempDir.resolve(fileName);
    Files.writeString(tempFilePath, combinedContent);

    // Upload the temporary file to the remote server
    sftpChannel.put(tempFilePath.toAbsolutePath().toString(), remoteScriptPath);

    // Clean up the temporary file safely using Files.delete() for better error handling
    try {
      Files.delete(tempFilePath);
      log.info("Pipeline combined file uploaded to server and temp file deleted successfully.");
    } catch (IOException e) {
      log.error("Failed to delete temp file: {}", tempFilePath, e);
    }

    // Clean up the temporary directory if needed
    try {
      Files.delete(tempDir);
    } catch (IOException e) {
      log.error("Failed to delete temp directory: {}", tempDir, e);
    }

    sftpChannel.disconnect();
  }


  /**
   * Executes commands. It creates a list of strings from the session.
   *
   * @param session  the session
   * @param commands the commands
   * @return the list of strings
   * @throws Exception if an error occurs while executing the commands
   */
  private static void executeCommands(ScriptOperation scriptOperation, Session session, List<String> commands, List<String> result) {
    commands.forEach((String command) ->
      CommonUtils.executeSingleCommand(scriptOperation, null, session, command, result));
  }



}
