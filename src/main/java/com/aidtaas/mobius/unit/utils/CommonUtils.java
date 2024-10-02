
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.InMemoryFile;
import com.aidtaas.mobius.unit.dto.ScriptOperation;
import com.aidtaas.mobius.unit.enums.ScriptSource;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.exception.StateLockException;
import com.aidtaas.mobius.unit.handlers.TerraformHandler;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

import static com.aidtaas.mobius.unit.constants.BobConstants.BYTE_SIZE;
import static org.camunda.bpm.engine.variable.type.ValueType.FILE;
import static org.camunda.bpm.engine.variable.type.ValueType.OBJECT;

/**
 * This class represents common utilities.
 * It includes a method to sanitize a value and a method to sanitize a process value.
 */
@Slf4j
public final class CommonUtils {

  private CommonUtils() {
  }

  /**
   * Sanitizes a process value.
   *
   * @param value the value
   * @return the sanitized process value
   */
  public static Object sanitizeProcessValue(TypedValue value) {
    Object result = null;
    if (ObjectUtils.isEmpty(value)) {
      return result;
    }

    if (FILE.equals(value.getType())) {
      result = new InMemoryFile((FileValue) value);
    } else if (OBJECT.equals(value.getType()) && value instanceof Collection<?> collection) {
      result = sanitizeCollection(collection);
    } else {
      result = value.getValue();
    }

    return result;
  }

  /**
   * Sanitizes a collection.
   *
   * @param collection the collection
   * @return the sanitized collection
   */
  private static Object sanitizeCollection(Collection<?> collection) {
    return collection.stream().filter(FileValue.class::isInstance).map(FileValue.class::cast)
      .findFirst().map(InMemoryFile::new).orElse(null);
  }

  /**
   * Gets the workflow ID.
   *
   * @param execution the execution
   * @return the workflow ID
   */
  public static String getWorkflowId(ExternalTask execution) {
    String processDefinitionKey = execution.getProcessDefinitionKey();
    return processDefinitionKey.substring(BobConstants.PROC_DEF_SUBSTRING);
  }

  public static String getWorkflowId(DelegateExecution execution) {
    String processDefinitionKey = execution.getProcessDefinitionId();
    return processDefinitionKey.substring(BobConstants.PROC_DEF_SUBSTRING);
  }

  public static int calculateRetries(ExternalTask externalTask, int retries) {
    if (ObjectUtils.isNotEmpty(externalTask.getRetries())) {
      retries = externalTask.getRetries();
    }
    if (retries <= 0) {
      return 0;
    }
    return retries - 1;
  }

  public static Map<String, Object> getStringObjectMap(String scriptVariables) {
    Map<String, Object> scriptVariablesMap;

    try {
      scriptVariablesMap = Config.OBJECT_MAPPER
        .readValue(scriptVariables, new TypeReference<>() {
        });
    } catch (JsonProcessingException e) {
      log.error("Error while parsing the script variables", e);
      throw new ObjectMappingException("Error while mapping the script variables", e);
    }
    return scriptVariablesMap;
  }

  /**
   * Prepares a JSch. It creates a JSch object and adds an identity if a private key path is
   * provided.
   *
   * @param privateKeyPath the private key path
   * @param password       the password
   * @return the JSch object
   */
  public static JSch prepareJSch(String privateKeyPath, String password) {
    JSch jsch = new JSch();
    if (StringUtils.isEmpty(privateKeyPath)) {
      return jsch;
    }
    try {
      if (privateKeyPath.startsWith("http")) {
        byte[] privateKeyBytes = readPrivateKeyFromUrl(privateKeyPath);
        jsch.addIdentity("PEM_KEY", privateKeyBytes, null,
          ObjectUtils.isEmpty(password) ? null : password.getBytes());
      } else {
        if (ObjectUtils.isEmpty(password)) {
          jsch.addIdentity((privateKeyPath));
        } else {
          jsch.addIdentity(privateKeyPath, password);
        }
      }
    } catch (IOException | JSchException e) {
      throw new NonRetryableException(e);
    }
    return jsch;
  }

  /**
   * Reads a private key from a URL. It gets the bytes from the URL and returns them.
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

  /**
   * Creates a session. It logs the user, private key path, host, port, and password.
   *
   * @param user           the user
   * @param privateKeyPath the private key path
   * @param host           the host
   * @param port           the port
   * @param password       the password
   * @param jsch           the JSch object
   * @return the session
   * @throws JSchException if an error occurs while creating the session
   */
  public static Session getSession(String user, String privateKeyPath, String host, int port,
                                   String password, JSch jsch) {
    try {
      log.info("--------------Creating a session with user {} host {} port {}", user, host, port);
      var session = jsch.getSession(user, host, port);
      if (ObjectUtils.isEmpty(privateKeyPath)) {
        session.setPassword(password);
      }
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();
      log.info("----------Session Got Connected--------------------");
      return session;
    } catch (JSchException e) {
      throw new NonRetryableException("Error connecting to the server ...", e);
    }
  }

  public static ScriptInputData getScriptInputData(ScriptOperation scriptOperation, Map<String, Object> inputFields) {
    String scriptPath = getRequiredField(inputFields, "scriptPath");

    log.info("Path : {}", scriptPath);

    long timeoutDuration = Optional.ofNullable(inputFields.get("timeout"))
      .map(Object::toString)
      .map(Long::parseLong)
      .map(timeoutInSeconds -> timeoutInSeconds * BobConstants.SEC_TO_MS_MULTIPLIER)
      .orElseGet(() -> {
        Instant lockExpirationTime = scriptOperation.getExternalTask().getLockExpirationTime().toInstant();
        Instant currentTime = Instant.now();
        return lockExpirationTime.toEpochMilli() - currentTime.toEpochMilli();
      });

    scriptOperation.setTimeoutDuration(timeoutDuration);
    scriptOperation.getExternalTaskService().extendLock(scriptOperation.getExternalTask(), timeoutDuration);

    Map<String, Object> finalScriptVariables = extractScriptVariables(inputFields);

    String scriptName = getRequiredField(inputFields, "scriptName");

    String staticPath = Optional.ofNullable(inputFields.get("staticPath"))
      .map(Object::toString)
      .orElse(scriptName);

    ScriptSource scriptSource = Optional.ofNullable(inputFields.get("scriptSource"))
      .map(source -> ScriptSource.valueOf(source.toString().toUpperCase(Locale.ROOT)))
      .orElse(ScriptSource.GIT);

    String privateKeyPath = Optional.ofNullable(inputFields.get("privateKeyPath"))
      .map(Object::toString)
      .orElse(null);

    String password = getRequiredField(inputFields, "password");
    String user = getRequiredField(inputFields, "user");
    String host = getRequiredField(inputFields, "host");
    int port = getRequiredFieldAsInt(inputFields, "port");
    String file = getRequiredField(inputFields, "scriptUrl");
    boolean create = isRequiredFieldBoolean(inputFields, "create");

    return new ScriptInputData(scriptName, scriptPath, scriptSource, privateKeyPath, password, user, host, port, file,
      create, staticPath, finalScriptVariables);
  }

  private static String getRequiredField(Map<String, Object> inputFields, String key) {
    return Optional.ofNullable(inputFields.get(key))
      .map(Object::toString)
      .orElseThrow(() -> new NonRetryableException(key + " cannot be null or empty"));
  }

  private static int getRequiredFieldAsInt(Map<String, Object> inputFields, String key) {
    return Optional.ofNullable(inputFields.get(key))
      .map(Object::toString)
      .map(Integer::parseInt)
      .orElseThrow(() -> new NonRetryableException(key + " cannot be null or empty"));
  }

  private static boolean isRequiredFieldBoolean(Map<String, Object> inputFields, String key) {
    return Optional.ofNullable(inputFields.get(key))
      .map(Object::toString)
      .map(Boolean::parseBoolean)
      .orElse(true);
  }

  private static Map<String, Object> extractScriptVariables(Map<String, Object> inputFields) {
    Object scriptVariables = inputFields.get("scriptVariables");

    if (ObjectUtils.isEmpty(scriptVariables)) {
      log.warn("scriptVariables are empty or null");
      return new HashMap<>();
    }

    if (scriptVariables instanceof String) {
      return CommonUtils.getStringObjectMap(scriptVariables.toString());
    } else {
      return (Map<String, Object>) scriptVariables;
    }
  }

  public static void readStream(InputStream stream, List<String> result) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        processLine(line, result);
      }
    } catch (IOException e) {
      log.error("Error while reading stream", e);
    }
  }

  private static void processLine(String line, List<String> result) {
    if (result != null) {
      log.info(line);
      result.add(line);
    } else {
      log.error("Error: {}", line);
      if (line.contains("state blob is already locked")) {
        throw new StateLockException(line);
      }
    }
  }

  public static void executeSingleCommand(ScriptOperation scriptOperation, String terraformPath, Session session,
                                          String command, List<String> result) {

    ChannelExec channel;
    InputStream inputStream;
    InputStream errorStream;
    ExecutorService executor;
    long timeoutMillis;
    try {
      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand(command);

      inputStream = channel.getInputStream();
      errorStream = channel.getErrStream();

      executor = Executors.newFixedThreadPool(TerraformHandler.LOG_READER_THREAD_POOL);
      timeoutMillis = scriptOperation.getTimeoutDuration();

      channel.connect();
    } catch (JSchException e) {
      String errorMessage = String.format("Error while executing the command: %s", command);
      throw new NonRetryableException(errorMessage, e);
    } catch (IOException e) {
      throw new NonRetryableException("Error reading the input stream...", e);
    }

    try {

      Future<?> outputFuture = executor.submit(() -> readStream(inputStream, result));
      Future<?> errorFuture = executor.submit(() -> readStream(errorStream, null));

      outputFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
      errorFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);

    } catch (TimeoutException e) {
      log.error("Command execution timed out due to inactivity. Forcefully disconnecting the channel.", e);
      deleteStateLock(scriptOperation, terraformPath, session);
      throw new NonRetryableException("Command execution timed out due to inactivity...", HttpStatus.SC_REQUEST_TIMEOUT);
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      log.error("Error executing command", e);
      deleteStateLock(scriptOperation, terraformPath, session);
    } finally {
      channel.disconnect();
      executor.shutdown();
    }

    log.info("Exit Status {}", channel.getExitStatus());
    if (channel.getExitStatus() != 0) {
      log.error("Command failed with exit status {}", channel.getExitStatus());
    }
  }

  private static void deleteStateLock(ScriptOperation scriptOperation, String terraformPath, Session session) {

    if (StringUtils.isEmpty(terraformPath)) {
      return;
    }
    log.info("----- Deleting the state lock -----");

    //TODO: Should come from vault secrets
    String storageAccountKey = "9GejUaGBY9Ce56nyDPHMXj4vZDvzetI8nuVxMMUM3VlVZzLdgTzb4B1M9sGdFvHv6ptoR8JnzX\\/K+AStvDxO4Q==";

    String blobCommand = String.format(BobConstants.STORAGE_BLOB_COMMAND_FORMAT, terraformPath + "/terraform.tfstate", storageAccountKey);
    String stateLeaseAndDeleteCommand = String.format(BobConstants.AZ_STORAGE_BLOB_COMMAND_FORMAT, blobCommand, blobCommand);

    CommonUtils.executeSingleCommand(scriptOperation, terraformPath, session, stateLeaseAndDeleteCommand, new ArrayList<>());

    log.info("----- Deleted the state lock successfully -----");
  }

  public record ScriptInputData(
    String scriptName,
    String scriptPath,
    ScriptSource scriptSource,
    String privateKeyPath,
    String password,
    String user,
    String host,
    int port,
    String file,
    boolean create,
    String staticPath,
    Map<String, Object> finalScriptVariables
  ) {
  }

}
