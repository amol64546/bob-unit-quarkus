
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.exception.ValidationException;
import com.aidtaas.mobius.unit.model.Workflow;
import com.aidtaas.mobius.unit.repositories.ActivityByteArrayRepo;
import com.aidtaas.mobius.unit.repositories.VariableInstanceRepo;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.aidtaas.mobius.unit.constants.BobConstants.*;

@Slf4j
@ApplicationScoped
public class WorkflowUtils {

  private final DynamicRestClient restClient;
  private final VariableInstanceRepo variableInstanceRepo;
  private final ActivityByteArrayRepo activityByteArrayRepo;

  public WorkflowUtils(DynamicRestClient restClient, VariableInstanceRepo variableInstanceRepo,
                       ActivityByteArrayRepo activityByteArrayRepo) {
    this.restClient = restClient;
    this.variableInstanceRepo = variableInstanceRepo;
    this.activityByteArrayRepo = activityByteArrayRepo;
  }

  /**
   * Method to get the latest workflow from the Camunda engine.
   *
   * @param url  the URL to get the latest workflow
   * @param auth the authorization token
   * @return the response body of the API call
   */
  public Workflow getLatestWorkflow(String url, String auth) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(AUTHORIZATION, auth);
    ApiResponseBody apiResponseBody = restClient.makeApiCall(url, null, GET, headers);
    try {
      return Config.OBJECT_MAPPER.readValue(apiResponseBody.getBody(), Workflow.class);
    } catch (JsonProcessingException e) {
      throw new ObjectMappingException("Error parsing latest workflow JSON", e);
    }
  }


  /**
   * Method to parse the XML and map the data.
   *
   * @param xmlString the XML string
   * @return the map of the output variables
   */
  public Map<String, String> parseXMLAndMapData(String xmlString) {
    try {
      var jsonObject = XML.toJSONObject(xmlString);
      var processObjects = jsonObject.getJSONObject(BobConstants.BPMN_DEFINITIONS_TAG)
        .getJSONObject(BobConstants.BPMN_PROCESS_TAG);
      return processObjects.has(BobConstants.BPMN_SERVICE_TASK_TAG)
        ? getOutputVariables(processObjects)
        : new HashMap<>();
    } catch (JSONException e) {
      log.error("Error parsing the XML: ", e);
      throw new ValidationException("Error parsing workflow xml");
    }
  }

  /**
   * Method to get the output variables from the process objects.
   *
   * @param processObjects the process objects
   * @return the map of the output variables
   */
  private static Map<String, String> getOutputVariables(JSONObject processObjects) {

    Map<String, String> outputVariablesMap = new HashMap<>();

    var serviceObject = processObjects.get(BobConstants.BPMN_SERVICE_TASK_TAG);
    JSONArray serviceTasks;
    if (serviceObject instanceof JSONObject) {
      serviceTasks = new JSONArray();
      serviceTasks.put(serviceObject);
    } else {
      serviceTasks = (JSONArray) serviceObject;
    }

    IntStream.range(0, serviceTasks.length()).mapToObj(serviceTasks::getJSONObject)
      .forEach((JSONObject serviceTask) -> getVariablesFromServiceTaskOutputs(serviceTask, outputVariablesMap));

    return outputVariablesMap;
  }

  /**
   * Method to get the variables from the service task outputs.
   *
   * @param serviceTask     the service task
   * @param outputVariables the output variables
   */
  private static void getVariablesFromServiceTaskOutputs(JSONObject serviceTask, Map<String, String> outputVariables) {
    var activityId = serviceTask.getString(BobConstants.ID_TAG);
    if (serviceTask.has(BPMN_EXTENSION_ELEMENTS_TAG)) {
      Object inputOutput = serviceTask.getJSONObject(BPMN_EXTENSION_ELEMENTS_TAG)
        .getJSONObject(CAMUNDA_INPUT_OUTPUT_TAG).get(CAMUNDA_INPUT_PARAMETER_TAG);
      //If inputOutput is JSONObject, it doesn't contain output parameters, then no need to set variables
      if (inputOutput instanceof JSONArray inputOutputArray) {
        var content = inputOutputArray.getJSONObject(1).getString(BobConstants.CONTENT_TAG);
        var contentObj = new JSONObject(content);
        JSONObject outputs = (JSONObject) contentObj.get(BobConstants.OUTPUTS_TAG);
        List<String> keys = new ArrayList<>(outputs.keySet());
        outputVariables.put(activityId, keys.getFirst());
      }
    }
  }

  /**
   * Method to get the data size from the variable map.
   *
   * @param outputVariablesMap the map of the output variables
   * @param processInstanceId  the process instance ID
   * @return the map of the activity data count
   */
  public Map<String, Long> getDataSizeFromVariableMap(Map<String, String> outputVariablesMap,
                                                      String processInstanceId) {

    Map<String, Long> activityDataCount = new HashMap<>();

    outputVariablesMap.entrySet().stream().forEach((Map.Entry<String, String> entry) ->
      getAndProcessVariableData(processInstanceId, entry, activityDataCount));
    return activityDataCount;
  }

  /**
   * Method to get and process the variable data.
   *
   * @param processInstanceId the process instance ID
   * @param entry             the entry of the output variables map
   * @param activityDataCount the map of the activity data count
   */
  private void getAndProcessVariableData(String processInstanceId, Map.Entry<String, String> entry,
                                         Map<String, Long> activityDataCount) {
    String activityId = entry.getKey();
    String outputVariable = entry.getValue();
    log.info("ActivityId: {}, OutputVariable: {}, processInstanceId: {}", activityId, outputVariable,
      processInstanceId);
    var dataCountDTO = variableInstanceRepo
      .findDataCountByProcessInstanceIdAndVariableName(processInstanceId, outputVariable);
    if (ObjectUtils.isEmpty(dataCountDTO)) {
      log.error("DataCountDTO is null for processInstanceId: {} and variableName: {}",
        processInstanceId, outputVariable);
      activityDataCount.put(activityId, 0L);
    } else {
      Optional<String> variableType = Optional.ofNullable(dataCountDTO.getText2());
      if (variableType.map(SPINJAR_COM_MINIDEV_JSON_JSONARRAY::equals).orElse(false)) {
        String variableBytearrayId = dataCountDTO.getBytearrayId();
        long variableDataSize = deserializeJSONArrayAndGetData(variableBytearrayId);
        activityDataCount.put(activityId, variableDataSize);
      } else {
        activityDataCount.put(activityId, 1L);
      }
    }
  }

  /**
   * Method to deserialize the JSONArray and get the data.
   *
   * @param variableBytearrayId the variable bytearray ID
   * @return the data size of the JSONArray
   */
  public long deserializeJSONArrayAndGetData(String variableBytearrayId) {

    byte[] variableByteArray = activityByteArrayRepo.findBytesByByteArrayId(variableBytearrayId);

    try (var byteArrayInputStream = new ByteArrayInputStream(variableByteArray);
         var objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

      var deserializedObject = objectInputStream.readObject();

      if (deserializedObject instanceof JSONArray jsonArray) {
        log.info("Deserialized Object is JSONArray type");
        return jsonArray.length();
      } else {
        return 1L;
      }

    } catch (IOException | ClassNotFoundException e) {
      log.error("Error deserializing the object: {}", e.getMessage());
      return 1L;
    }
  }
}
