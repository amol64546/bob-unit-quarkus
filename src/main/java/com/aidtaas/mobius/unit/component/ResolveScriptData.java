
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.component;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.Field;
import com.aidtaas.mobius.unit.dto.InputAttribute;
import com.aidtaas.mobius.unit.dto.ScriptOperation;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.enums.SourceType;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.PathNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

/**
 * This class represents a ResolveData handler.
 * It is annotated with @Slf4j and @ApplicationScoped.
 * These annotations provide a logger and specify that the handler is application-scoped.
 * The handler includes methods to resolve all API data, resolve HTTP method,
 * resolve URL, resolve headers parameters, and resolve body.
 */
@Slf4j
@ApplicationScoped
@AllArgsConstructor
public class ResolveScriptData {

  private static final Map<SourceType, Resolver> resolvers = new EnumMap<>(SourceType.class);

  @PostConstruct
  public void init() {

    resolvers.put(SourceType.STATIC, (ScriptOperation scriptOperation, Object fieldValue, String label,
                                      String parentJsonPath, Map<String, InputAttribute> attributes) -> fieldValue);
    resolvers.put(SourceType.PROPERTY, (ScriptOperation scriptOperation, Object fieldValue, String label,
                                        String parentJsonPath, Map<String, InputAttribute> attributes) ->
      resolveProperty(scriptOperation, fieldValue, parentJsonPath));
    resolvers.put(SourceType.GLOBALS, (ScriptOperation scriptOperation, Object fieldValue, String label,
                                       String parentJsonPath, Map<String, InputAttribute> attributes) ->
      scriptOperation.getRunTimeVariable(label));
    resolvers.put(SourceType.SCRIPT, (ScriptOperation scriptOperation, Object fieldValue, String label,
                                      String parentJsonPath, Map<String, InputAttribute> attributes) ->
      scriptOperation.getRunTimeVariable(label));
    resolvers.put(SourceType.EXTERNALS, (ScriptOperation scriptOperation, Object fieldValue, String label,
                                         String parentJsonPath, Map<String, InputAttribute> attributes) ->
      scriptOperation.getRunTimeVariable(label));
    resolvers.put(SourceType.GROUP, (ScriptOperation scriptOperation, Object fieldValue, String label,
                                     String parentJsonPath, Map<String, InputAttribute> attributes) ->
      resolveGroup(scriptOperation, fieldValue, attributes));
  }

  @FunctionalInterface
  private interface Resolver {
    Object resolve(ScriptOperation scriptOperation, Object fieldValue, String label,
                   String parentJsonPath, Map<String, InputAttribute> attributes);
  }

  /**
   * Resolves all API data.
   * It logs the start and end of the resolution, builds the RestApi object, and logs any exceptions that occur.
   *
   * @param scriptOperation the API operation
   * @return the RestApi object
   * @throws ObjectMappingException if an error occurs while resolving the API data
   */
  public Map<String, Object> resolveScriptVariables(ScriptOperation scriptOperation) {
    try {
      log.info("Resolving all Script inputs for activity {} of component {}",
        scriptOperation.getInput().getActivityId(), scriptOperation.getInput().getComponentId());
      Map<String, Object> resolvedVariables = new HashMap<>();
      scriptOperation.getInput().getFields().forEach((String key, Field value) -> {
        log.info("Resolving Script input {} with value {}", key, value);
        Object resolvedValue = resolveFields(scriptOperation, key);
        resolvedVariables.put(key, resolvedValue);
      });
      return resolvedVariables;

    } catch (Exception resolutionError) {
      var errorMessage = String.format("Failed to resolve script inputs %s: %s ",
        scriptOperation.getInput(), resolutionError.getMessage());
      throw new NonRetryableException(errorMessage, resolutionError);
    }
  }

  /**
   * Resolves the headers parameters.
   * It logs the retrieval of the headers parameters and returns them.
   *
   * @param scriptOperation the Script operation
   * @param fieldName       the field name
   * @return the headers parameters
   */
  private static Object resolveFields(ScriptOperation scriptOperation, String fieldName) {
    log.info(BobConstants.RETRIEVE, fieldName, scriptOperation.getInput().getFields().get(fieldName));

    return ofNullable(resolveData(scriptOperation,
      scriptOperation.getInput().getFields().get(fieldName)))
      .map(Object.class::cast).orElse(null);
  }

  /**
   * Resolves the data for a given input field.
   *
   * @param inputField the input field
   * @return the resolved data
   */
  public static Object resolveData(ScriptOperation scriptOperation, Field inputField) {
    return Optional.ofNullable(inputField).filter(field -> ObjectUtils.isNotEmpty(field.getType())).map(
      field -> resolvers.get(field.getType()).resolve(scriptOperation, field.getValue(),
        field.getValue().toString().substring(1), "", field.getItems())).orElse(null);
  }

  /**
   * Resolves the property for a given field value and parent JSON path.
   *
   * @param fieldValue     the field value
   * @param parentJsonPath the parent JSON path
   * @return the resolved property
   */
  private static Object resolveProperty(ScriptOperation scriptOperation, Object fieldValue, String parentJsonPath) {
    // existing code for PROPERTY case
    String configKey = Environment.PROD.getValue().equalsIgnoreCase(scriptOperation.getEnvironment().getValue())
      ? "allianceConfig" : "masterConfig";
    String configPath = "listenerConfig";
    String[] fieldPathSegments = Optional.ofNullable(configPath)
      .map((String path) -> String.format("%s#%s#children", configKey, path)).orElse(configKey)
      .concat(Optional.of(parentJsonPath + fieldValue).map(String.class::cast).orElse(""))
      .concat("#value").split("#");
    log.info("Field segments for property {}", (Object) fieldPathSegments);

    String formattedFieldPath = stream(fieldPathSegments)
      .map(pathSegment -> StringUtils.isNumeric(pathSegment) ? pathSegment : String.format("'%s'", pathSegment))
      .map(pathSegment -> String.format("[%s]", pathSegment)).collect(Collectors.joining("."));

    log.info("Formatted path for property {}", formattedFieldPath);
    return scriptOperation.getProductJson().read(formattedFieldPath);
  }

  /**
   * Resolves the group for a given field value.
   *
   * @param fieldValue the field value
   * @param attributes the attributes
   * @return the resolved group
   */
  private static Object resolveGroup(ScriptOperation scriptOperation, Object fieldValue,
                                     Map<String, InputAttribute> attributes) {
    return Optional.ofNullable(fieldValue)
      .map(String.class::cast)
      .map(group -> processGroup(scriptOperation, group, attributes))
      .orElse(Collections.emptyMap());
  }

  /**
   * Processes the group for a given group and attributes.
   *
   * @param group      the group
   * @param attributes the attributes
   * @return the processed group
   */
  private static Object processGroup(ScriptOperation scriptOperation, String group,
                                     Map<String, InputAttribute> attributes) {
    if (MapUtils.isNotEmpty(attributes)) {
      return attributes.entrySet().parallelStream()
        .filter(entry -> ObjectUtils.isNotEmpty(entry.getValue().getType()))
        .map(entry -> processEntry(scriptOperation, entry, group))
        .filter(entry -> ObjectUtils.isNotEmpty(entry.getValue()))
        .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    } else {
      return Collections.emptyMap();
    }
  }

  /**
   * Processes the entry for a given entry and group.
   *
   * @param entry the entry
   * @param group the group
   * @return the processed entry
   */
  private static Map.Entry<String, Object> processEntry(ScriptOperation scriptOperation, Map.Entry<String,
    InputAttribute> entry, String group) {
    String attributeName = entry.getKey();
    InputAttribute attribute = entry.getValue();
    Object attributeValue = resolvers.get(attribute.getType()).resolve(scriptOperation, attribute.getValue(),
      attribute.getLabel(), group, null);
    return new AbstractMap.SimpleEntry<>(attributeName, attributeValue);
  }

  /**
   * Processes the output for a given body and Script operation.
   *
   * @param body            the body
   * @param scriptOperation the Script operation
   */
  public void processOutput(String body, ScriptOperation scriptOperation) {

    AtomicReference<DocumentContext> responseJson = new AtomicReference<>(BobConstants.EMPTY_JSON_CONTEXT);

    try {
      if (ObjectUtils.isNotEmpty(body)) {
        responseJson.set(parse(body));
      }
    } catch (InvalidJsonException parseException) {
      throw new NonRetryableException(String.format("Error parsing json response body : %s ", body),
        parseException);
    }
    scriptOperation.getOutput().getItems()
      .forEach((variable, attribute) -> ofNullable(attribute.getProperty())
        .map(ResolveScriptData::normalizePath)
        .ifPresent(path -> processPath(path, variable, responseJson, scriptOperation)));
  }

  private static String normalizePath(String path) {
    if ("_$".equals(path)) {
      path = "$";
    }
    return path.replace("#", ".");
  }

  private void processPath(String path, String variable, AtomicReference<DocumentContext> responseJson, ScriptOperation scriptOperation) {
    try {
      Object value = responseJson.get().read(path);
      scriptOperation.getRuntimeVariables().put(variable, Config.OBJECT_MAPPER.writeValueAsString(value));
    } catch (PathNotFoundException e) {
      log.warn("Path {} not found in response body", path, e);
      scriptOperation.getRuntimeVariables().put(variable, null);
    } catch (JsonProcessingException e) {
      throw new NonRetryableException("Error serializing JSON value", e);
    }
  }

}
