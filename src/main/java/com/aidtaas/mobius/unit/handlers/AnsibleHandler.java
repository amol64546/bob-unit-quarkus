
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.handlers;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.Operation;
import com.aidtaas.mobius.unit.utils.CommonUtils;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;

/**
 * This class represents an Ansible handler. It is annotated with @Slf4j and @ApplicationScoped.
 * These annotations provide a logger and specify that the handler is application-scoped. The
 * handler includes a method to execute an Ansible command.
 */
@Slf4j
@ApplicationScoped
public class AnsibleHandler  implements ExternalTaskHandler, JavaDelegate {


  /**
   * Sends a message to the server. It logs the response from the server.
   */
  @Override
  public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
    log.info("************* AnsibleScriptHandler **************");

    var operation = new Operation(externalTask, externalTaskService);
    String fileUrl = operation.getCastedRunTimeVariable("$_ANSIBLE_FILE_URL", String.class);
    String resultVariable = operation.getCastedRunTimeVariable("$_ANSIBLE_SCRIPT_RESULT",
      String.class);
    String user = operation.getCastedRunTimeVariable("$_USER_ANSIBLE", String.class);
    String privateKeyPath = operation.getCastedRunTimeVariable("$_PRIVATE_KEY_PATH_ANSIBLE",
      String.class);
    String host = operation.getCastedRunTimeVariable("$_HOST_ANSIBLE", String.class);
    int port = operation.getCastedRunTimeVariable("$_PORT_ANSIBLE", Integer.class);
    String password = operation.getCastedRunTimeVariable("$_PASSWORD_ANSIBLE", String.class);

    String scriptVariables = operation.getCastedRunTimeVariable(
      "$_ANSIBLE_SCRIPT_VARIABLES", String.class);

    Map<String, List<String>> scriptVariablesMap = getStringListMap(scriptVariables);

    List<String> result = null;
    try {
      result = executeCommand(user, privateKeyPath, host, port, fileUrl, password,
        scriptVariablesMap);
    } catch (Exception e) {
      log.error("Error while executing the command", e);
      externalTaskService.handleFailure(externalTask,
        "Failed to execute the commands || " + e.getMessage(),
        String.valueOf(HttpURLConnection.HTTP_INTERNAL_ERROR), 0, 0);
    }

    log.info("Execution Result: ");

    Map<String, Object> variables = new HashMap<>();
    variables.put(resultVariable, result);
    externalTaskService.setVariables(externalTask, variables);

    externalTaskService.complete(externalTask);
    log.info("************* End AnsibleScriptHandler **************");
  }


  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {

  }


  static Map<String, List<String>> getStringListMap(String jsonString) {
    Map<String, List<String>> scriptVariablesMap;

    try {
      scriptVariablesMap = Config.OBJECT_MAPPER
        .readValue(jsonString, new TypeReference<>() {
        });
    } catch (JsonProcessingException e) {
      throw new ObjectMappingException("Error while mapping the script variables", e);
    }

    return scriptVariablesMap;
  }

  /**
   * Executes a command. It logs the parameters, gets a session, creates a list of commands, and
   * returns the strings from the session.
   *
   * @param user           the user
   * @param privateKeyPath the path of the private key
   * @param host           the host
   * @param port           the port
   * @param file           the file
   * @param password       the password
   * @return the strings from the session
   * @throws Exception if an error occurs while executing the command
   */
  public List<String> executeCommand(String user, String privateKeyPath, String host, int port, String file,
                                     String password, Map<String, List<String>> scriptVariablesMap) throws Exception {
    log.info("user :{},  privateKeyPath:{},  host:{},  port:{},  file:{},  password :{}", user,
      privateKeyPath,
      host, port, file, password);

    String hostip = scriptVariablesMap.get("hostip").get(0);
    String sshuser = scriptVariablesMap.get("sshuser").get(0);
    String sshpass = scriptVariablesMap.get("sshpass").get(0);

    var servicesString = String.join(", ", scriptVariablesMap.get("services"));
    var formattedServices = String.format("{ %s }", servicesString);

    var jsch = CommonUtils.prepareJSch(privateKeyPath, password);
    var session = CommonUtils.getSession(user, privateKeyPath, host, port, password, jsch);

    String newfolder = "Ansible_Gaian_" + Instant.now().toEpochMilli();

    List<String> commands = new ArrayList<>();
    commands.add("cd RUNRUN_Dont-Delete_Ask-Sharan/");
    commands.add("cd RUNRUN_Dont-Delete_Ask-Sharan/" + " && mkdir " + newfolder);

    String folderName = "RUNRUN_Dont-Delete_Ask-Sharan/" + newfolder + "/";

    commands.add("cd " + folderName + " && git clone " + file);
    commands.add(
      "cd " + folderName + BobConstants.RUN_RUN_INSTALL_SERVICES + "&& sed -i 's/\\$hostip/" + hostip
        + "/g; s/\\$sshuser/" + sshuser + "/g; s/\\$sshpass/" + sshpass + "/g' hosts.ini");
    commands.add("cd " + folderName + BobConstants.RUN_RUN_INSTALL_SERVICES + "&&  sed -i 's/\\$services/"
      + formattedServices + "/' services.yml");

    commands.add(
      "cd " + folderName + BobConstants.RUN_RUN_INSTALL_SERVICES + "&&" + " ansible-playbook main.yml");

    return getStrings(session, commands, log);
  }

  static List<String> getStrings(Session session, List<String> commands, Logger log)
    throws JSchException, IOException {
    ArrayList<String> result = new ArrayList<>();
    for (String command : commands) {
      // Create a channel to execute the command
      var channelExec = (ChannelExec) session.openChannel("exec");
      channelExec.setCommand(command);

      // Connect the channel
      channelExec.connect();

      // Get the command's output
      var inputStream = channelExec.getInputStream();
      var reader = new BufferedReader(new InputStreamReader(inputStream));

      // Read and print the output line by line
      String line;
      while ((line = reader.readLine()) != null) {
        result.add(line);
      }

      // Disconnect the channel
      channelExec.disconnect();
    }

    log.info("-------------Disconnection session-------------------------");
    session.disconnect();
    return result;
  }

}
