
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.component;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiOperation;
import com.aidtaas.mobius.unit.dto.Field;
import com.aidtaas.mobius.unit.dto.InputAttribute;
import com.aidtaas.mobius.unit.dto.RestApi;
import com.aidtaas.mobius.unit.enums.ContentType;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.enums.SourceType;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.utils.JsonUtils;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import io.quarkus.vault.VaultKVSecretEngine;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import static com.aidtaas.mobius.unit.constants.BobConstants.*;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
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
public class ResolveData {

  private static final Map<ContentType, BiFunction<ApiOperation, String, Object>> processorMap
    = new EnumMap<>(ContentType.class);
  private static final Map<SourceType, Resolver> resolvers = new EnumMap<>(SourceType.class);
  private final VaultKVSecretEngine vaultKVSecretEngine;


  @PostConstruct
  public void init() {
    processorMap.put(ContentType.APPLICATION_JSON, ResolveData::processJsonContentType);
    processorMap.put(ContentType.APPLICATION_X_WWW_FORM_URLENCODED, ResolveData::processFormContentType);
    processorMap.put(ContentType.MULTIPART_FORM_DATA, ResolveData::processFormContentType);
    processorMap.put(ContentType.APPLICATION_OCTET_STREAM, ResolveData::processOctetStreamContentType);
    processorMap.put(ContentType.APPLICATION_XML, ResolveData::processXmlContentType);
    processorMap.put(ContentType.TEXT_PLAIN, ResolveData::processPlainTextContentType);

    resolvers.put(SourceType.STATIC, (ApiOperation apiOperation, Object fieldValue, String label,
                                      String parentJsonPath, Map<String, InputAttribute> attributes) -> fieldValue);
    resolvers.put(SourceType.PROPERTY, (ApiOperation apiOperation, Object fieldValue, String label,
                                        String parentJsonPath, Map<String, InputAttribute> attributes) ->
      resolveProperty(apiOperation, fieldValue, parentJsonPath));
    resolvers.put(SourceType.GLOBALS, (ApiOperation apiOperation, Object fieldValue, String label,
                                       String parentJsonPath, Map<String, InputAttribute> attributes) ->
      apiOperation.getRunTimeVariable(label));
    resolvers.put(SourceType.SCRIPT, (ApiOperation apiOperation, Object fieldValue, String label,
                                      String parentJsonPath, Map<String, InputAttribute> attributes) ->
      apiOperation.getRunTimeVariable(label));
    resolvers.put(SourceType.EXTERNALS, (ApiOperation apiOperation, Object fieldValue, String label,
                                         String parentJsonPath, Map<String, InputAttribute> attributes) ->
      apiOperation.getRunTimeVariable(label));
    resolvers.put(SourceType.GROUP, (ApiOperation apiOperation, Object fieldValue, String label,
                                     String parentJsonPath, Map<String, InputAttribute> attributes) ->
      resolveGroup(apiOperation, fieldValue, attributes));
  }

  @FunctionalInterface
  private interface Resolver {
    Object resolve(ApiOperation apiOperation, Object fieldValue, String label,
                   String parentJsonPath, Map<String, InputAttribute> attributes);
  }

  /**
   * Resolves all API data.
   * It logs the start and end of the resolution, builds the RestApi object, and logs any exceptions that occur.
   *
   * @param apiOperation the API operation
   * @return the RestApi object
   * @throws ObjectMappingException if an error occurs while resolving the API data
   */
  public RestApi resolveAllApiData(ApiOperation apiOperation) {

    log.info("Resolving all REST API inputs for activity {} of component {}",
      apiOperation.getInput().getActivityId(), apiOperation.getInput().getComponentId());

    String secretsPath = apiOperation.getExecutorTenantId() + PATH_DELIMITER + apiOperation.getProductId() + "/MasterConfig";
    Map<String, String> secrets = new HashMap<>();
    try {
      secrets = vaultKVSecretEngine.readSecret(secretsPath);
    } catch (RuntimeException e) {
      log.error("Error reading secrets from vault", e);
    }
    apiOperation.setSecrets(secrets);

    try {
      String httpMethod = resolveHttpMethod(apiOperation);
      String url = resolveUrl(apiOperation, URL);
      Map<String, String> headers = resolveHeadersParams(apiOperation, HTTP_HEADERS);
      Map<String, String> pathParams = resolveHeadersParams(apiOperation, PATH_PARAMETERS);
      Map<String, String> queryParams = resolveHeadersParams(apiOperation, QUERY_PARAM);

      if (StringUtils.isEmpty(headers.get(CONTENT_TYPE))) {
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
      }

      Object body = resolveBody(apiOperation, headers.get(CONTENT_TYPE), HTTP_PAYLOAD);

      return new RestApi(httpMethod, url, queryParams, pathParams, headers, body);

    } catch (RuntimeException resolutionError) {
      var errorMessage = String.format("Failed to resolve rest api inputs %s: %s ",
        apiOperation.getInput(), resolutionError.getMessage());
      throw new NonRetryableException(errorMessage, resolutionError);
    }
  }

  /**
   * Resolves the HTTP method.
   * It logs the retrieval of the HTTP method and returns it.
   *
   * @param apiOperation the API operation
   * @return the HTTP method
   * @throws NonRetryableException if the HTTP method is null
   */
  private static String resolveHttpMethod(ApiOperation apiOperation) {
    log.info(BobConstants.RETRIEVE, BobConstants.HTTP_METHOD,
      apiOperation.getInput().getFields().get(BobConstants.HTTP_METHOD));

    return ofNullable(resolveData(apiOperation,
      apiOperation.getInput().getFields().get(BobConstants.HTTP_METHOD)))
      .map(String.class::cast).orElseThrow(() -> new NonRetryableException("HTTP METHOD is null! "));
  }

  /**
   * Resolves the URL.
   * It logs the retrieval of the URL and returns it.
   *
   * @param apiOperation the API operation
   * @param fieldName    the field name
   * @return the URL
   * @throws RuntimeException if the URL is null
   */
  private static String resolveUrl(ApiOperation apiOperation, String fieldName) {
    log.info(BobConstants.RETRIEVE, fieldName, apiOperation.getInput().getFields().get(fieldName));

    return ofNullable(resolveData(apiOperation, apiOperation.getInput().getFields()
      .get(fieldName))).map(String.class::cast)
      .map(httpUrl -> httpUrl.split("\\?")[0])
      .orElseThrow(() -> new RuntimeException("HTTP URL is null! "));
  }

  /**
   * Resolves the headers parameters.
   * It logs the retrieval of the headers parameters and returns them.
   *
   * @param apiOperation the API operation
   * @param fieldName    the field name
   * @return the headers parameters
   */
  private static Map<String, String> resolveHeadersParams(ApiOperation apiOperation, String fieldName) {
    log.info(BobConstants.RETRIEVE, fieldName, apiOperation.getInput().getFields().get(fieldName));

    return ofNullable(resolveData(apiOperation, apiOperation.getInput().getFields().get(fieldName)))
      .map(params -> (Map<String, String>) params).orElse(Collections.emptyMap());
  }

  /**
   * Resolves the body.
   * It logs the retrieval of the body and returns it.
   * The body is processed differently depending on the content type.
   *
   * @param apiOperation the API operation
   * @param contentType  the content type
   * @param fieldName    the field name
   * @return the body
   */
  public static Object resolveBody(ApiOperation apiOperation, String contentType, String fieldName) {
    ContentType type;
    try {
      type = ContentType.valueOf(contentType.replace('/', '_')
        .replace('-', '_').toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      log.warn("Unknown content type: {}", contentType, e);
      throw new NonRetryableException("Unknown content type provided");
    }
    log.info(BobConstants.RETRIEVE, fieldName, apiOperation.getInput().getFields().get(fieldName));
    return processorMap.getOrDefault(type, (apiOp, fName) -> null).apply(apiOperation, fieldName);
  }

  /**
   * Processes the plain text content type.
   * It retrieves the data and returns it.
   *
   * @param apiOperation the API operation
   * @param fieldName    the field name
   * @return the data
   */
  private static Object processPlainTextContentType(ApiOperation apiOperation, String fieldName) {
    Optional<Object> resolvedData = ofNullable(resolveData(apiOperation, apiOperation.getInput().getFields().get(fieldName)));
    if (resolvedData.isPresent()) {
      var text = resolvedData.get().toString();
      return text.substring(text.indexOf("=") + 1, resolvedData.get().toString().length() - 1);
    } else {
      return null;
    }
  }

  /**
   * Processes the XML content type.
   * It retrieves the data and returns it.
   *
   * @param apiOperation the API operation
   * @param fieldName    the field name
   * @return the data
   */
  private static Object processXmlContentType(ApiOperation apiOperation, String fieldName) {
    Optional<Object> resolvedData = ofNullable(resolveData(apiOperation, apiOperation.getInput()
      .getFields().get(fieldName)));
    if (resolvedData.isPresent()) {
      Map<String, Object> hashMap = (ConcurrentHashMap<String, Object>) resolvedData.get();
      return hashMap.values().iterator().next().toString();
    } else {
      return null;
    }
  }

  /**
   * Processes the octet stream content type.
   * It retrieves the file and returns it.
   *
   * @param apiOperation the API operation
   * @param fieldName    the field name
   * @return the file
   */
  private static Object processOctetStreamContentType(ApiOperation apiOperation, String fieldName) {
    Optional<Object> file = ofNullable(resolveData(apiOperation, apiOperation.getInput().getFields().get(fieldName)))
      .map(params -> (Map<String, Object>) params).flatMap(params -> params.values().parallelStream()
        .findAny());
    if (file.isPresent()) {
      return file.get();
    } else {
      return null;
    }
  }

  /**
   * Processes the form content type.
   * It retrieves the form data and returns it.
   *
   * @param apiOperation the API operation
   * @param fieldName    the field name
   * @return the form data
   */
  private static Object processFormContentType(ApiOperation apiOperation, String fieldName) {
    Map<String, Object> formData = new HashMap<>();
    ofNullable(resolveData(apiOperation, apiOperation.getInput().getFields().get(fieldName)))
      .map(params -> (Map<String, Object>) params)
      .ifPresent(formData::putAll);

    return formData;
  }

  /**
   * Processes the JSON content type.
   * It retrieves the JSON data and returns it.
   *
   * @param apiOperation the API operation
   * @param fieldName    the field name
   * @return the JSON data
   */
  private static Object processJsonContentType(ApiOperation apiOperation, String fieldName) {
    return ofNullable(resolveData(apiOperation, apiOperation.getInput().getFields().get(fieldName)))
      .map(params -> (Map<String, Object>) params).map(JsonUtils::generateJson)
      .orElseGet(Config.OBJECT_MAPPER::createObjectNode);
  }


  /**
   * Resolves the data for a given input field.
   *
   * @param inputField the input field
   * @return the resolved data
   */
  public static Object resolveData(ApiOperation apiOperation, Field inputField) {
    return Optional.ofNullable(inputField).filter(field -> ObjectUtils.isNotEmpty(field.getType())).map(
      field -> resolvers.get(field.getType()).resolve(apiOperation, field.getValue(),
        field.getValue().toString().substring(1), "", field.getItems())).orElse(null);
  }

  /**
   * Resolves the property for a given field value and parent JSON path.
   *
   * @param fieldValue     the field value
   * @param parentJsonPath the parent JSON path
   * @return the resolved property
   */
  private static Object resolveProperty(ApiOperation apiOperation, Object fieldValue, String parentJsonPath) {
    // existing code for PROPERTY case
    String configKey = Environment.PROD.getValue().equalsIgnoreCase(apiOperation.getEnvironment().getValue())
      ? "allianceConfig" : "masterConfig";
    String[] fieldPathSegments = Optional.ofNullable(apiOperation.getInput().getApiPath())
      .map(path -> String.format("%s#%s#children", configKey, path)).orElse(configKey)
      .concat(Optional.of(parentJsonPath + fieldValue).map(String.class::cast).orElse(""))
      .concat("#value").split("#");
    log.info("Field segments for property {}", (Object) fieldPathSegments);

    String formattedFieldPath = stream(fieldPathSegments)
      .map(pathSegment -> StringUtils.isNumeric(pathSegment) ? pathSegment : String.format("'%s'", pathSegment))
      .map(pathSegment -> String.format("[%s]", pathSegment)).collect(Collectors.joining("."));

    log.info("Formatted path for property {}", formattedFieldPath);
    Object valueFromProductJson = apiOperation.getProductJson().read(formattedFieldPath);
    if ("secret::sensitive_data".equals(valueFromProductJson.toString()) && MapUtils.isNotEmpty(apiOperation.getSecrets())) {
      return apiOperation.getSecrets().get(fieldValue.toString().substring(1));
    } else {
      return valueFromProductJson;
    }
  }

  /**
   * Resolves the group for a given field value.
   *
   * @param fieldValue the field value
   * @param attributes the attributes
   * @return the resolved group
   */
  private static Object resolveGroup(ApiOperation apiOperation, Object fieldValue,
                                     Map<String, InputAttribute> attributes) {
    return Optional.ofNullable(fieldValue)
      .map(String.class::cast)
      .map(group -> processGroup(apiOperation, group, attributes))
      .orElse(Collections.emptyMap());
  }

  /**
   * Processes the group for a given group and attributes.
   *
   * @param group      the group
   * @param attributes the attributes
   * @return the processed group
   */
  private static Object processGroup(ApiOperation apiOperation, String group, Map<String, InputAttribute> attributes) {
    if (MapUtils.isNotEmpty(attributes)) {
      return attributes.entrySet().parallelStream()
        .filter(entry -> ObjectUtils.isNotEmpty(entry.getValue().getType()))
        .map(entry -> processEntry(apiOperation, entry, group))
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
  private static Map.Entry<String, Object> processEntry(ApiOperation apiOperation, Map.Entry<String,
    InputAttribute> entry, String group) {
    String attributeName = entry.getKey();
    InputAttribute attribute = entry.getValue();
    Object attributeValue = resolvers.get(attribute.getType()).resolve(apiOperation, attribute.getValue(),
      attribute.getLabel(), group, null);
    return new AbstractMap.SimpleEntry<>(attributeName, attributeValue);
  }

}
