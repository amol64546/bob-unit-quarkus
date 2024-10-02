
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiOperation;
import com.aidtaas.mobius.unit.dto.ApiOperationInput;
import com.aidtaas.mobius.unit.dto.ApiOperationOutput;
import com.aidtaas.mobius.unit.dto.Field;
import com.aidtaas.mobius.unit.dto.OutputAttribute;
import com.aidtaas.mobius.unit.dto.ScriptOperation;
import com.aidtaas.mobius.unit.dto.ScriptOperationInput;
import com.aidtaas.mobius.unit.dto.ScriptOperationOutput;
import com.aidtaas.mobius.unit.enums.ApiType;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toConcurrentMap;

/**
 * This class represents a Gaian BPMN validator implementation.
 * It is annotated with @Slf4j and @ApplicationScoped.
 * These annotations provide a logger and specify that the validator is application-scoped.
 * The validator includes methods to validate an API operation and a REST API operation.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class BobValidator {

  private Set<String> mandatoryFields;

  @PostConstruct
  public void init() {
    mandatoryFields = newHashSet("HTTP_METHOD", "URL");
  }

  /**
   * Validates an API operation.
   * It logs the start and end of the validation, builds the ApiOperationInput and
   * ApiOperationOutput objects, and logs any exceptions that occur.
   *
   * @param apiOperation the API operation
   * @throws ValidationException if an error occurs while validating the API operation
   */
  public void validateApiOperation(ApiOperation apiOperation) {

    var execution = apiOperation.getExternalTask();
    ApiOperationInput input = apiOperation.getInput();
    ApiOperationOutput output = apiOperation.getOutput();

    try {
      String processInstanceId = execution.getProcessInstanceId();
      String activityName = execution.getTopicName();
      String activityId = ofNullable(execution.getActivityId())
        .orElseThrow(() -> new NonRetryableException("Activity Id not found!"));

      log.info(BobConstants.VALIDATING_INPUTS_FOR_ACTIVITY_OF_PROCESS_INSTANCE, activityId, processInstanceId);

      String inputKey = format(BobConstants.INPUT_KEY_FORMAT, activityId);

      String inputJson = ofNullable(execution.getVariable(inputKey)).map(String.class::cast)
        .orElseThrow(() -> getException("Input Model", activityName, activityId, processInstanceId));

      Map<String, Object> inputs = Config.OBJECT_MAPPER.readValue(inputJson, Map.class);

      log.info("Api input for activity {} of process {} : {} ", activityId, processInstanceId, inputs);

      input.setActivityId(activityId);

      input.setComponentId(ofNullable(inputs.get(BobConstants.COMPONENT_ID))
        .map(String.class::cast).orElseThrow(() ->
          getException(BobConstants.COMPONENT_ID, activityName, activityId, processInstanceId)));

      input.setComponentName(ofNullable(inputs.get(BobConstants.COMPONENT_NAME)).map(String.class::cast)
        .orElse(BobConstants.API_OPERATION_HANDLER));

      input.setProductMasterConfigId(ofNullable(inputs.get(BobConstants.PRODUCT_MASTER_CONFIG_ID))
        .map(String.class::cast).orElseThrow(() ->
          getException(BobConstants.PRODUCT_MASTER_CONFIG_ID, activityName, activityId, processInstanceId)));

      input.setApiType(ofNullable(inputs.get(BobConstants.INTERFACE_TYPE)).map(String.class::cast)
        .map(ApiType::valueOf).orElse(ApiType.REST));

      input.setApiPath(ofNullable(inputs.get(BobConstants.INTERFACE_PATH)).map(String.class::cast)
        .map(path -> path.startsWith("#") ? path.substring(1) : path).orElseThrow(() ->
          getException(BobConstants.INTERFACE_PATH, activityName, activityId, processInstanceId)));

      input.setFields(ofNullable(inputs.get(BobConstants.ITEMS)).map(Map.class::cast).map(items ->

        ((Map<String, Field>) items).entrySet().parallelStream().map((Entry<String, Field> field) ->
            new SimpleEntry<>(field.getKey(), Config.OBJECT_MAPPER.convertValue(field.getValue(), Field.class)))
          .filter(ObjectUtils::isNotEmpty).filter(field -> Objects.nonNull(field.getValue().getType()))
          .collect(toConcurrentMap(Entry::getKey, Entry::getValue))

      ).orElseThrow(() -> getException("Fields", activityName, activityId, processInstanceId)));

      log.info("Validating outputs for activity {} of process instance {}", activityId, processInstanceId);

      String outputKey = format(BobConstants.OUTPUT_KEY_FORMAT, activityId);

      ofNullable(execution.getVariable(outputKey)).map(String.class::cast).ifPresent((String outputJson) ->
        resolveOutputRestApiOperation(outputJson, output));

      if (!input.getFields().keySet().containsAll(mandatoryFields)) {
        throw new NonRetryableException("Either HTTP_METHOD or URL is missing! ");
      }

      log.info(BobConstants.VALIDATING_INPUTS_FOR_ACTIVITY_OF_PROCESS_INSTANCE, activityId, processInstanceId);

    } catch (Exception exception) {
      throw new NonRetryableException("Error validating input data" + " || " + exception.getMessage(), exception);
    }
  }

  /**
   * Resolves the output REST API operation.
   * It logs the output REST API operation and sets the output attributes.
   *
   * @param outputJson the output JSON
   * @param output     the output REST API operation
   */
  private static void resolveOutputRestApiOperation(String outputJson, ApiOperationOutput output) {
    Map<String, Object> outputs;
    try {
      outputs = Config.OBJECT_MAPPER.readValue(outputJson, Map.class);
    } catch (JsonProcessingException e) {
      log.error("Error parsing output json: ", e);
      return;
    }

    ofNullable(outputs.get(BobConstants.OUTPUTS)).map(Map.class::cast).map(
        variables -> ((Map<String, Object>) variables).entrySet().parallelStream()
          .map((Entry<String, Object> variable) -> new SimpleEntry<>(variable.getKey(),
            Config.OBJECT_MAPPER.convertValue(variable.getValue(), OutputAttribute.class))).filter(ObjectUtils::isNotEmpty)
          .collect(toConcurrentMap(Entry::getKey, Entry::getValue)))
      .ifPresent(output::setItems);
  }

  public void validateScriptOperation(ScriptOperation scriptOperation) {

    var execution = scriptOperation.getExternalTask();
    ScriptOperationInput input = scriptOperation.getInput();
    ScriptOperationOutput output = scriptOperation.getOutput();

    try {
      String processInstanceId = execution.getProcessInstanceId();
      String activityName = execution.getTopicName();
      String activityId = ofNullable(execution.getActivityId())
        .orElseThrow(() -> new NonRetryableException("Activity Id not found!"));

      log.info(BobConstants.VALIDATING_INPUTS_FOR_ACTIVITY_OF_PROCESS_INSTANCE, activityId, processInstanceId);

      String inputKey = format(BobConstants.INPUT_KEY_FORMAT, activityId);

      String inputJson = ofNullable(execution.getVariable(inputKey)).map(String.class::cast)
        .orElseThrow(() -> getException("Input Model", activityName, activityId, processInstanceId));

      Map<String, Object> inputs = Config.OBJECT_MAPPER.readValue(inputJson, Map.class);

      log.info("Script input for activity {} of process {} : {} ", activityId, processInstanceId, inputs);

      input.setComponentId(ofNullable(inputs.get(BobConstants.COMPONENT_ID))
        .map(String.class::cast).orElseThrow(() ->
          getException(BobConstants.COMPONENT_ID, activityName, activityId, processInstanceId)));

      input.setComponentName(ofNullable(inputs.get(BobConstants.COMPONENT_NAME)).map(String.class::cast)
        .orElse(BobConstants.TERRAFORM_HANDLER));

      input.setProductMasterConfigId(ofNullable(inputs.get(BobConstants.PRODUCT_MASTER_CONFIG_ID))
        .map(String.class::cast).orElseThrow(() ->
          getException(BobConstants.PRODUCT_MASTER_CONFIG_ID, activityName, activityId, processInstanceId)));

      input.setInterfacePath(ofNullable(inputs.get(BobConstants.INTERFACE_PATH)).map(String.class::cast)
        .orElseThrow(() ->
          getException(BobConstants.COMPONENT_ID, activityName, activityId, processInstanceId)));

      input.setFields(ofNullable(inputs.get(BobConstants.ITEMS)).map(Map.class::cast).map(items ->
        ((Map<String, Field>) items).entrySet().parallelStream().map((Entry<String, Field> field) ->
            new SimpleEntry<>(field.getKey(), Config.OBJECT_MAPPER.convertValue(field.getValue(), Field.class)))
          .filter(ObjectUtils::isNotEmpty).filter(field -> Objects.nonNull(field.getValue().getType()))
          .collect(toConcurrentMap(Entry::getKey, Entry::getValue))
      ).orElseThrow(() -> getException("Fields", activityName, activityId, processInstanceId)));

      log.info(BobConstants.VALIDATING_INPUTS_FOR_ACTIVITY_OF_PROCESS_INSTANCE, activityId, processInstanceId);

      String outputKey = format(BobConstants.OUTPUT_KEY_FORMAT, activityId);

      ofNullable(execution.getVariable(outputKey)).map(String.class::cast).ifPresent((String outputJson) ->
        resolveOutputScriptOperation(outputJson, output));

    } catch (Exception exception) {
      throw new NonRetryableException("Error validating input data" + " || " + exception.getMessage(), exception);
    }
  }

  /**
   * Resolves the output REST API operation.
   * It logs the output REST API operation and sets the output attributes.
   *
   * @param outputJson the output JSON
   * @param output     the output REST API operation
   */
  private static void resolveOutputScriptOperation(String outputJson, ScriptOperationOutput output) {
    Map<String, Object> outputs;
    try {
      outputs = Config.OBJECT_MAPPER.readValue(outputJson, Map.class);
    } catch (JsonProcessingException e) {
      log.error("Error parsing output json: ", e);
      return;
    }

    ofNullable(outputs.get(BobConstants.OUTPUTS)).map(Map.class::cast).map(
        variables -> ((Map<String, Object>) variables).entrySet().parallelStream()
          .map((Entry<String, Object> variable) -> new SimpleEntry<>(variable.getKey(),
            Config.OBJECT_MAPPER.convertValue(variable.getValue(), OutputAttribute.class))).filter(ObjectUtils::isNotEmpty)
          .collect(toConcurrentMap(Entry::getKey, Entry::getValue)))
      .ifPresent(output::setItems);
  }

  /**
   * Returns an exception with a formatted message.
   * The message includes the item, activity name, activity ID, and process instance ID.
   *
   * @param item              the item
   * @param activityName      the activity name
   * @param activityId        the activity ID
   * @param processInstanceId the process instance ID
   * @return the exception
   */
  protected Exception getException(String item, String activityName, String activityId, String processInstanceId) {
    return new NonRetryableException(format("%s missing for activity %s - %s of process instance %s ! ", item,
      activityName, activityId, processInstanceId));
  }
}
