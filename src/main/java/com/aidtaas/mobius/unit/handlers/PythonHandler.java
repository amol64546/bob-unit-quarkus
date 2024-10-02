
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.handlers;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.Operation;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import static com.aidtaas.mobius.unit.constants.BobConstants.BYTE_SIZE;

/**
 * This class represents a Python handler.
 * It is annotated with @Slf4j and @ApplicationScoped.
 * These annotations provide a logger and specify that the handler is application-scoped.
 * The handler includes a method to execute a Python script.
 */
@Slf4j
@ApplicationScoped
public class PythonHandler  implements ExternalTaskHandler, JavaDelegate {

  /**
   * Executes an external task.
   * It logs the start and end of the execution, builds the execution variable script,
   * and logs any exceptions that occur.
   * It also logs the name and ID of the current thread.
   *
   * @param externalTask        the external task
   * @param externalTaskService the external task service
   */
  @Override
  public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
    log.info("************* PythonHandler **************");

    var operation = new Operation(externalTask, externalTaskService);
    String executionVariableScript = executionVariableScriptBuilder(externalTask);
    log.info("executionVariableScript :" + executionVariableScript);
    log.info("executionVariableScript :" + executionVariableScript);

    String user = operation.getCastedRunTimeVariable("$_USER", String.class);
    String privateKeyPath = operation.getCastedRunTimeVariable("$_PRIVATE_KEY_PATH", String.class);
    String host = operation.getCastedRunTimeVariable("$_HOST", String.class);
    int port = operation.getCastedRunTimeVariable("$_PORT", Integer.class);
    String password = operation.getCastedRunTimeVariable("$_PASSWORD", String.class);
    String pythonScript = operation.getCastedRunTimeVariable("$_SCRIPT", String.class);
    String resultVariable = operation.getCastedRunTimeVariable("$_RESULT_VARIABLE", String.class);

    // checking is there any pip lines in the top and extracting that pip lines from
    // script
    List<String> pipInstallLines = pipLines(pythonScript);
    // Remove the pip install lines from the script
    pythonScript = BobConstants.PIP_INSTALL_PATTERN.matcher(pythonScript).replaceAll("");
    // adding the workflow variables to python
    pythonScript = executionVariableScript + "\n" + (StringUtils.isEmpty(pythonScript) ? "" : pythonScript);

    Map<String, Object> result = null;
    try {
      result = installPython(user, privateKeyPath, host, port, password, pythonScript,
        pipInstallLines);
    } catch (Exception e) {
      log.error("Error while installing python", e);
      externalTaskService.handleFailure(externalTask,
        "Failed to install python || " + e.getMessage(),
        String.valueOf(HttpURLConnection.HTTP_INTERNAL_ERROR), 0, 0);
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put(resultVariable, result);
    externalTaskService.setVariables(externalTask, variables);

    externalTaskService.complete(externalTask);
    log.info("************* End PythonHandler **************");

  }

  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {

  }

  /**
   * Installs Python and executes a command.
   * It logs the parameters, gets a session, creates a list of commands, and returns the strings from the session.
   *
   * @param user            the user
   * @param privateKeyPath  the path of the private key
   * @param host            the host
   * @param port            the port
   * @param password        the password
   * @param pythonScript    the Python script
   * @param pipInstallLines the pip install lines
   * @return the strings from the session
   * @throws Exception if an error occurs while executing the command
   */
  public Map<String, Object> installPython(String user, String privateKeyPath, String host, int port, String password,
                                           String pythonScript, Collection<String> pipInstallLines) throws Exception {
    log.info(" user :{},  privateKeyPath:{},  host:{},  port:{}, password :{}, pythonScript : {}", user,
      privateKeyPath, host, port, password, pythonScript);
    var jsch = new JSch();
    configureJsch(jsch, privateKeyPath, password);

    var session = startSession(jsch, user, host, port, privateKeyPath, password);
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    installPythonIfNecessary(channel);

    if (CollectionUtils.isNotEmpty(pipInstallLines)) {
      for (String pipLine : pipInstallLines) {
        executePipCommand(channel, pipLine);
      }
    }
    Map<String, Object> pythonResult = executeCommand(pythonScript, session);
    channel.disconnect();
    session.disconnect();
    return pythonResult;
  }

  /**
   * Configures the JSch object.
   * It adds an identity if a private key path is provided.
   *
   * @param jsch           the JSch object
   * @param privateKeyPath the path of the private key
   * @param password       the password
   * @throws Exception if an error occurs while configuring the JSch object
   */
  private void configureJsch(JSch jsch, String privateKeyPath, String password) throws Exception {
    if (StringUtils.isNotEmpty(privateKeyPath)) {
      if (privateKeyPath.startsWith("http")) {
        log.info(privateKeyPath);
        byte[] privateKeyBytes = readPrivateKeyFromUrl(privateKeyPath);
        jsch.addIdentity("PEM_KEY", privateKeyBytes, null,
          StringUtils.isEmpty(password) ? null : password.getBytes());
      } else {
        addIdentity(jsch, privateKeyPath, password);
      }
    }
  }

  /**
   * Adds an identity to the JSch object.
   * It adds the identity with the provided private key path and password.
   *
   * @param jsch           the JSch object
   * @param privateKeyPath the path of the private key
   * @param password       the password
   * @throws JSchException if an error occurs while adding the identity
   */
  private static void addIdentity(JSch jsch, String privateKeyPath, String password) throws JSchException {
    if (StringUtils.isEmpty(password)) {
      jsch.addIdentity((privateKeyPath));
    } else {
      jsch.addIdentity(privateKeyPath, password);
    }
  }

  /**
   * Starts a session.
   * It creates a session with the provided user, host, and port.
   * It sets the password if a private key path is not provided.
   *
   * @param jsch           the JSch object
   * @param user           the user
   * @param host           the host
   * @param port           the port
   * @param privateKeyPath the path of the private key
   * @param password       the password
   * @return the session
   * @throws JSchException if an error occurs while starting the session
   */
  private static Session startSession(JSch jsch, String user, String host, int port, String privateKeyPath,
                                      String password) throws JSchException {
    var session = jsch.getSession(user, host, port);
    if (StringUtils.isEmpty(privateKeyPath)) {
      session.setPassword(password);
    }
    session.setConfig("StrictHostKeyChecking", "no");
    session.connect();
    return session;
  }

  /**
   * Installs Python if it is not already installed.
   * It checks if Python is installed by running a command.
   * If Python is not installed, it installs Python.
   *
   * @param channel the channel
   * @throws Exception if an error occurs while installing Python
   */
  private void installPythonIfNecessary(ChannelExec channel) throws Exception {
    channel.setCommand("python3 --version");
    channel.connect();
    var inputStream = channel.getInputStream();
    String output = readStream(inputStream);
    channel.disconnect();
    if (!output.toLowerCase(Locale.ENGLISH).contains("python")) {
      installPython(channel);
    } else {
      log.info("--Python is installed.--");
    }
  }

  /**
   * Installs Python.
   * It updates the package list and installs Python.
   *
   * @param channel the channel
   * @throws Exception if an error occurs while installing Python
   */
  private void installPython(ChannelExec channel) throws Exception {
    log.info("Python is not installed, Installing...");
    channel.setCommand("sudo apt-get update && sudo apt-get install python3 -y");
    channel.connect();
    channel.disconnect();
  }

  /**
   * Executes a pip command.
   * It sets the command, connects to the channel, reads the input stream,
   * logs the status, and disconnects from the channel.
   *
   * @param channel the channel
   * @param pipLine the pip command
   * @throws Exception if an error occurs while executing the pip command
   */
  private void executePipCommand(ChannelExec channel, String pipLine) throws Exception {
    channel.setCommand(pipLine);
    try {
      channel.connect();
      var inputStream = channel.getInputStream();
      String output = readStream(inputStream);
      int exitStatus = channel.getExitStatus();
      logStatus(exitStatus, output, pipLine);
    } catch (JSchException | IOException e) {
      log.error("error while executing the pip command", e);
    } finally {
      channel.disconnect();
    }
  }

  /**
   * Logs the status of a command.
   * It logs the exit status, output, and command.
   *
   * @param exitStatus the exit status
   * @param output     the output
   * @param pipLine    the command
   */
  private void logStatus(int exitStatus, String output, String pipLine) {
    if (exitStatus == 0) {
      log.info("Command executed successfully: " + pipLine);
      log.info("Output: " + output);
    } else {
      log.error("Error executing command: " + pipLine);
      log.error("Error output: " + output);
    }
  }

  /**
   * Executes a command.
   * It sets the command, connects to the channel, reads the input stream, and returns the variables.
   *
   * @param script  the script
   * @param session the session
   * @return the variables
   * @throws Exception if an error occurs while executing the command
   */
  private Map<String, Object> executeCommand(String script, Session session) throws Exception {
    log.info("script :" + script);
    Map<String, Object> variables = null;
    // script for all global variables
    var scriptForOutputJson = """
      import json
          
      globals_dict = dict(globals())
      variables = {}
      for var_name in globals_dict:
          if (
              not var_name.startswith("__")
              and not callable(globals_dict[var_name])
              and var_name != "execution"
              and var_name != "json"
          ):
              variables[var_name] = globals_dict[var_name]
      output = json.dumps(variables, default=str)
      print(output)
      """;

    script += scriptForOutputJson;
    var pythonExecutable = "python3";

    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    String command = pythonExecutable + " -W ignore -c \"" + script.replace("\"", "\\\"") + "\"";
    channel.setCommand(command);

    var inputStream = channel.getInputStream();
    channel.connect();

    String output = readStream(inputStream);
    int exitCode = channel.getExitStatus();
    if (exitCode == 0) {
      Config.OBJECT_MAPPER.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);
      variables = Config.OBJECT_MAPPER.readValue(output, new TypeReference<>() {
      });
      log.info("Variables: {}", variables);

    } else {
      log.info("Python script returned an error: {}", output);

    }

    return variables;
  }

  // converting privatePathKey to byte Array

  /**
   * Reads a private key from a URL.
   * It gets the bytes from the URL and returns them.
   *
   * @param privateKeyPathUrl the URL of the private key
   * @return the bytes of the private key
   * @throws IOException if an error occurs while reading the private key
   */
  private static byte[] readPrivateKeyFromUrl(String privateKeyPathUrl) throws IOException {
    var url = URI.create(privateKeyPathUrl).toURL();
    var outputStream = new ByteArrayOutputStream();
    try (var inputStream = url.openStream()) {
      var buffer = new byte[BYTE_SIZE];
      int length;
      while ((length = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, length);
      }
    }
    return outputStream.toByteArray();
  }

  // For Adding the workflow varibales to python environment

  /**
   * Builds a script to set the execution variables.
   * It gets the variables from the external task and builds a script to set them.
   *
   * @param externalTask the external task
   * @return the script to set the execution variables
   */
  public String executionVariableScriptBuilder(ExternalTask externalTask) {
    Map<String, Object> variables = externalTask.getAllVariables();
    var scriptBuilder = new StringBuilder("execution={");
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      String key = entry.getKey();
      if (!"$_SCRIPT".equals(key)) {
        Object value = entry.getValue();
        scriptBuilder.append("'").append(key).append("': ");
        if (value instanceof String) {
          scriptBuilder.append("'").append(value).append("'");
        } else {
          scriptBuilder.append(value);
        }
        scriptBuilder.append(", ");

      }

    }
    scriptBuilder.append("}");
    return scriptBuilder.toString().replace(", }", "}");

  }

  /**
   * Reads a stream.
   * It reads the input stream and returns the string.
   *
   * @param inputStream the input stream
   * @return the string
   * @throws IOException if an error occurs while reading the stream
   */
  private static String readStream(InputStream inputStream) throws IOException {
    var reader = new BufferedReader(new InputStreamReader(inputStream));
    var stringBuilder = new StringBuilder();
    String line;
    while (StringUtils.isNotEmpty(line = reader.readLine())) {
      stringBuilder.append(line).append('\n');
    }
    return stringBuilder.toString();
  }

  // For install libraries ,getting install commands From Script

  /**
   * Gets the pip install lines from a script.
   * It gets the lines that start with "pip install" from the script.
   *
   * @param script the script
   * @return the pip install lines
   */
  private static List<String> pipLines(String script) {
    List<String> pipInstallLines = new ArrayList<>();
    pipInstallLines.add("sudo apt-get update && sudo apt-get install python3-pip -y");
    String[] lines = BobConstants.LINE_SPLIT_PATTERN.split(script);

    List<String> pipInstallCommands = Arrays.stream(lines)
      .takeWhile(line -> line.contains("pip install"))
      .toList();

    pipInstallLines.addAll(pipInstallCommands);
    return pipInstallLines;
  }


}
