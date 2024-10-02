
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.dto.DeploymentResponse;
import com.aidtaas.mobius.unit.dto.ProcessInstanceResponse;
import com.aidtaas.mobius.unit.exception.ApiException;
import com.aidtaas.mobius.unit.exception.ValidationException;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.aidtaas.mobius.unit.constants.BobConstants.*;

@Slf4j
@ApplicationScoped
public class CamundaUtils {

  private final DynamicRestClient restClient;

  private final ConfigProperties engineRestConfig;

  public CamundaUtils(DynamicRestClient restClient,
                      ConfigProperties engineRestConfig) {
    this.restClient = restClient;
    this.engineRestConfig = engineRestConfig;
  }

  /**
   * Deploys the workflow to Camunda.
   *
   * @param xml      The XML content of the workflow.
   * @param id       The ID of the workflow.
   * @param tenantId
   * @return The response from deploying the workflow.
   * @throws IOException If there is an error while deploying the workflow.
   */
  public DeploymentResponse deployToCamunda(String xml, String id, String tenantId) throws IOException {
    DeploymentResponse deploymentResponse;
    String camundaDeploymentUrl = engineRestConfig.engineRestUrl() + "/deployment/create";
    log.info("camundaDeploymentUrl: {}", camundaDeploymentUrl);

    // Create a secure directory specific to your application
    var secureTempDir = new File(System.getProperty("java.io.tmpdir"), "secureAppDir");
    if (!secureTempDir.exists()) {
      secureTempDir.mkdir();
    }

    // Create the temp file in the secure directory
    var tempFile = File.createTempFile(GAIANWORKFLOW, BPMN, secureTempDir);

    writeToFile(tempFile, xml);

    //Replace below hardcoded values with actual values
    var entityBuilder = MultipartEntityBuilder.create();
    entityBuilder.addTextBody(DEPLOYMENT_SOURCE, DEPLOYMENT_SOURCE_VALUE);
    entityBuilder.addTextBody(ENABLE_DUPLICATE_FILTERING, TRUE);
    entityBuilder.addTextBody(DEPLOYMENT_NAME, GAIANWORKFLOW + id + BPMN);
    entityBuilder.addTextBody(DEPLOYMENT_ACTIVATION_TIME,
      OffsetDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN)));
    //Here, filename should be same as the file name given while creating tempFile with prefix and suffix
    entityBuilder.addBinaryBody(DATA, tempFile, ContentType.APPLICATION_XML, GAIANWORKFLOW + BPMN);

    HttpEntity entity = entityBuilder.build();
    var httpPost = new HttpPost(camundaDeploymentUrl);
    httpPost.setEntity(entity);
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpContext context = new BasicHttpContext();
    deploymentResponse = httpClient.execute(httpPost, context, this::getDeploymentResponse);
    log.info("end of deployToCamunda method of CamundaHelperUtils class with deploymentResponse: {}",
      deploymentResponse);
    return deploymentResponse;
  }

  /**
   * Gets the deployment response from the HTTP response.
   *
   * @param response The HTTP response.
   * @return The deployment response.
   * @throws IOException    If there is an error while reading the response.
   * @throws ParseException If there is an error while parsing the response.
   */
  private DeploymentResponse getDeploymentResponse(ClassicHttpResponse response) throws IOException, ParseException {
    HttpEntity responseEntity = response.getEntity();
    if (response.getCode() == HTTP_STATUS_CODE_200) {
      return Config.OBJECT_MAPPER.readValue(EntityUtils.toString(responseEntity), DeploymentResponse.class);
    } else {
      var errorResponse = EntityUtils.toString(responseEntity);
      Map<String, String> errorMessage = Config.OBJECT_MAPPER.readValue(errorResponse, Map.class);
      if (MapUtils.isEmpty(errorMessage)) {
        throw new ApiException(errorResponse);
      }
      throw new ValidationException(errorMessage.get("message"));
    }
  }

  /**
   * Writes the modified BPMN/XML to a file.
   *
   * @param tempFile The temporary file to write to.
   * @param xml      The modified BPMN/XML.
   */
  private void writeToFile(File tempFile, String xml) {
    try (var writer = new FileWriter(tempFile)) {
      writer.write(xml);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new ObjectMappingException("Error while writing the modified bpmn/xml to file : " + e.getMessage(), e);
    }
  }

  /**
   * Starts a process instance in Camunda.
   *
   * @param wfId      The ID of the workflow.
   * @param variables The variables to pass to the process instance.
   * @param auth      The authorization token for the requester.
   * @return The response from starting the process instance.
   */
  public ProcessInstanceResponse startProcessInstance(String wfId, Map<String, Object> variables, String auth) {
    String startProcessInstanceUrl = engineRestConfig.engineRestUrl() +
      "/process-definition/key/" + GAIANWORKFLOWS + wfId + "/start";
    log.info("URL for starting process instance: {}", startProcessInstanceUrl);
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> variableTypeKeyValueFormat = prepareRequestBody(variables);
    requestBody.put(VARIABLES, variableTypeKeyValueFormat);
    requestBody.put(BUSINESS_KEY, GAIANWORKFLOWS);
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(ACCEPT, APPLICATION_JSON);
    headers.put(AUTHORIZATION, auth);
    ApiResponseBody response = restClient.makeApiCall(startProcessInstanceUrl, requestBody, POST, headers);
    try {
      return Config.OBJECT_MAPPER.readValue(response.getBody(),
        ProcessInstanceResponse.class);
    } catch (JsonProcessingException e) {
      log.error("Error while reading api response from execute workflow", e);
      throw new ObjectMappingException("Error reading api response from execute workflow");
    }
  }

  /**
   * Prepares the request body for starting a process instance in Camunda.
   *
   * @param variables The variables to pass to the process instance.
   * @return The request body in the required format.
   */
  private static Map<String, Object> prepareRequestBody(Map<String, Object> variables) {
    Map<String, Object> newVariablesKeyTypeValueFormat = new HashMap<>();
    variables.entrySet().stream().forEach((Map.Entry<String, Object> entry) ->
      checkValueTypeAndConstructVariableMap(entry, newVariablesKeyTypeValueFormat));
    return newVariablesKeyTypeValueFormat;
  }

  /**
   * Checks the value type and constructs the variable map.
   *
   * @param entry                          The entry in the variables map.
   * @param newVariablesKeyTypeValueFormat The new variables map in the required format.
   */
  private static void checkValueTypeAndConstructVariableMap(Map.Entry<String, Object> entry,
                                                            Map<String, Object> newVariablesKeyTypeValueFormat) {
    Object value = entry.getValue();
    Map<String, Object> variableValue = new HashMap<>();
    if (value instanceof InputStream inputStream) {
      var encodedFile = Base64.getEncoder().encodeToString(readInputStreamToByteArray(inputStream));
      variableValue.put(VALUE, encodedFile);
      variableValue.put(TYPE, FILE);
      Map<String, String> valueInfo = new HashMap<>();
      valueInfo.put(FILENAME, entry.getKey());
      valueInfo.put(MIMETYPE, APPLICATION_OCTET_STREAM);
      valueInfo.put(ENCODING, UTF_8);
      variableValue.put(VALUE_INFO, valueInfo);
    } else if (value instanceof List) {
      variableValue.put(VALUE, value);
      variableValue.put(TYPE, JSON);
    } else {
      variableValue.put(VALUE, value.toString());
      variableValue.put(TYPE, STRING);
    }
    newVariablesKeyTypeValueFormat.put(entry.getKey(), variableValue);
  }

  /**
   * Reads the input stream to a byte array.
   *
   * @param inputStream The input stream to read.
   * @return The byte array representation of the input stream.
   */
  private static byte[] readInputStreamToByteArray(InputStream inputStream) {
    try {
      return inputStream.readAllBytes();
    } catch (IOException e) {
      throw new ApiException("Failed to read InputStream to byte array", e);
    }
  }

  /**
   * Convert the multipart form data input to a map.
   *
   * @param fileInput the file input
   * @param inputs    the inputs
   * @return the map
   */
  public void convertMultipartFormDataInputToMap
  (MultipartFormDataInput fileInput, Map<String, Object> inputs, Map<String, InputPart> fileInputs) {

    fileInput.getFormDataMap().entrySet().stream().forEach((Map.Entry<String, List<InputPart>> entry) ->
      entry.getValue().stream().forEach((InputPart inputPart) ->
        validateInputPart(inputs, entry, inputPart, fileInputs)));
  }

  /**
   * Validate the input part.
   *
   * @param inputs    the inputs
   * @param entry     the entry
   * @param inputPart the input part
   * @param result    the result
   */
  private void validateInputPart(Map<String, Object> inputs, Map.Entry<String, List<InputPart>> entry,
                                 InputPart inputPart, Map<String, InputPart> result) {

    String contentType = inputPart.getMediaType().getType();

    if (contentType.startsWith("text") || StringUtils.isEmpty(contentType)) {
      try {
        var value = inputPart.getBodyAsString();
        processInputValues(entry.getKey(), value, inputs);
      } catch (IOException e) {
        log.error("Error while validating input data: ", e);
        throw new ValidationException("Error processing input form data");
      }
    } else {
      result.put(entry.getKey(), inputPart);
    }
  }

  /**
   * Processes the input values for the workflow variables.
   *
   * @param variable  The variable for the workflow.
   * @param value     The value for the workflow.
   * @param variables The variables for the workflow.
   */
  private void processInputValues(String variable, String value,
                                  Map<String, Object> variables) {
    if (isJson(value)) {
      try {
        Object data = Config.OBJECT_MAPPER.readTree(value);
        variables.put(variable, data);
      } catch (JsonProcessingException e) {
        throw new ObjectMappingException("error parsing json", e);
      }
    } else {
      variables.put(variable, value);
    }
  }

  /**
   * Processes the file inputs for the workflow variables.
   *
   * @param fileInputs The file inputs for the workflow.
   * @param variables  The variables for the workflow.
   */
  public void processFileInputs(Map<String, InputPart> fileInputs, Map<String, Object> variables) {
    fileInputs.entrySet().stream().forEach((Map.Entry<String, InputPart> entry) -> {
      try {
        InputStream value = entry.getValue().getBody(InputStream.class, null);
        variables.put(entry.getKey(), value);
      } catch (IOException e) {
        log.error("Error while processing file inputs", e);
        throw new ApiException("Error processing input file data", e);
      }
    });
  }

  /**
   * Method to get the latest deployed version of the workflow from the Camunda engine.
   *
   * @param url  the URL to get the latest deployed version
   * @param auth the authorization token
   * @return the latest deployed version of the workflow
   */
  public Integer getWorkflowLatestDeployedVersionApiCall(String url, String auth) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(AUTHORIZATION, auth);
    String response = restClient.makeApiCall(url, null, GET, headers).getBody();
    return StringUtils.isEmpty(response) ? 1 : Integer.parseInt(response);
  }

  public boolean isJson(Object data) {

    if (ObjectUtils.isNotEmpty(data)) {
      try {
        Config.OBJECT_MAPPER.readTree(data.toString());
        return true;
      } catch (IOException e) {
        log.debug("Error while reading JSON data: ", e);
        return false;
      }
    }
    return false;
  }
}
