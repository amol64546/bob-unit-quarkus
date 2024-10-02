
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.component;

import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiOperation;
import com.aidtaas.mobius.unit.dto.ApiResponse;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.grpc.ApiMetering;
import com.aidtaas.mobius.unit.grpc.GrpcClient;
import com.aidtaas.mobius.unit.utils.TfUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.camunda.bpm.client.task.ExternalTask;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION_GLOBAL;
import static com.aidtaas.mobius.unit.constants.BobConstants.CONTENT_TYPE;
import static com.aidtaas.mobius.unit.utils.MeteringUtils.newMeteringStat;
import static com.aidtaas.mobius.unit.utils.MeteringUtils.newMeteringStatGrpc;
import static com.jayway.jsonpath.JsonPath.parse;

/**
 * This class is responsible for managing API response callbacks.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ApiResponseCallBack {

  private final TfUtils tfUtils;

  private final GrpcClient grpcClient;

  private final ConfigProperties config;

  /**
   * Sends a message back to the external task service with the API response.
   *
   * @param externalTask the external task
   * @param apiResponse  the API response
   */
  public void sendMsgBack(ExternalTask externalTask, ApiResponse apiResponse) {

    log.info("-------start-------Thread name and id at ApiResponseCallBack, {} {}",
      Thread.currentThread().getName(), Thread.currentThread().threadId());

    settingApiResponse(apiResponse.getApiResponseBody(), apiResponse.getResolvedUrl(),
      apiResponse.getHttpMethod(), apiResponse.getApiOperation());

    if (Environment.PROD.getValue().equalsIgnoreCase(apiResponse.getApiOperation().getEnvironment().getValue())) {
      apiMetering(externalTask, apiResponse.getApiOperation(), apiResponse.getApiResponseBody(),
        apiResponse.getBody(), apiResponse.getResolvedUrl(), apiResponse.getHttpMethod(), apiResponse.getHeaders());
    }

    log.info("-------end-------Thread name and id at ApiResponseCallBack, sendMsgBack, {} {}",
      Thread.currentThread().getName(), Thread.currentThread().threadId());
  }

  /**
   * Handles API metering.
   *
   * @param externalTask the external task
   * @param apiOperation the API operation
   * @param apiResponse  the API response body
   * @param body         the body of the API response
   * @param resolvedUrl  the resolved URL
   * @param httpMethod   the HTTP method
   * @param headers
   */
  private void apiMetering(ExternalTask externalTask, ApiOperation apiOperation, ApiResponseBody apiResponse,
                           Object body, String resolvedUrl, String httpMethod, Map<String, String> headers) {

    var apiMeteringDTO = newMeteringStat(apiOperation, externalTask);
    apiMeteringDTO.setApiProduct(resolvedUrl);
    apiMeteringDTO.setApiMethod(httpMethod);
    apiMeteringDTO.setApiBody(body);
    apiMeteringDTO.setResponseStatus(apiResponse.getStatusCodeValue());
    apiMeteringDTO.setApiResponse(apiResponse);

    if ((apiResponse.getStatusCodeValue() >= BobConstants.HTTP_STATUS_CODE_200 &&
      apiResponse.getStatusCodeValue() < BobConstants.HTTP_STATUS_CODE_300)
      && ObjectUtils.isNotEmpty(apiResponse.getBody())) {
      apiMeteringDTO.setApiResponseBodySize(apiResponse.getBody().getBytes().length);
    }
    if (ObjectUtils.isNotEmpty(body)) {
      apiMeteringDTO.setApiRequestBodySize(body.toString().getBytes().length);
    }
    apiMeteringDTO.setTenantType(BobConstants.TP_TENANT);
    tfUtils.sendToTF(apiMeteringDTO, externalTask.getVariable(AUTHORIZATION_GLOBAL).toString(), config.apiMeteringDtoSchemaId());
    if (!resolvedUrl.contains(config.serviceDomain())) {
      ApiMetering newMeteringStatGrpc = newMeteringStatGrpc(apiOperation, externalTask, body,
        resolvedUrl, httpMethod, apiResponse, headers);

      log.info("-----calling grpc method for metering");
      grpcClient.sendMessage(newMeteringStatGrpc);
    }
  }

  /**
   * Sets the response for the signal.
   *
   * @param apiResponse  the API response body
   * @param resolvedUrl  the resolved URL
   * @param httpMethod   the HTTP method
   * @param apiOperation the API operation
   */
  private void settingApiResponse(ApiResponseBody apiResponse, String resolvedUrl,
                                  String httpMethod, ApiOperation apiOperation) {

    apiOperation.getRuntimeVariables().put(String.format(BobConstants.CURRENT_RESPONSE_CODE,
      apiOperation.getInput().getActivityId()), apiResponse.getStatusCodeValue());
    apiOperation.getRuntimeVariables().put(String.format(BobConstants.API_RESPONSE,
      apiOperation.getInput().getActivityId()), String.format("Rest api call %s %s responded with code %s ",
      httpMethod, resolvedUrl, apiResponse.getStatusCodeValue()));

    settingDynamicContentResponse(apiResponse, apiOperation);

  }

  /**
   * Sets the dynamic content response.
   *
   * @param apiResponse  the API response body
   * @param apiOperation the API operation
   */
  private void settingDynamicContentResponse(ApiResponseBody apiResponse, ApiOperation apiOperation) {

    AtomicReference<DocumentContext> responseJson = new AtomicReference<>(BobConstants.EMPTY_JSON_CONTEXT);

    String contentType = ObjectUtils.isNotEmpty(apiResponse.getHeaders().get(CONTENT_TYPE))
      ? apiResponse.getHeaders().get(CONTENT_TYPE).toString()
      : "";
    if (ObjectUtils.isNotEmpty(apiOperation.getOutput().getItems()) && ObjectUtils.isNotEmpty(apiResponse.getBody())) {
      if (contentType.contains(MediaType.APPLICATION_XML)) {
        log.info("Processing XML output of type {}", contentType);
        processingXmlResponse(responseJson, apiResponse, apiOperation);
      } else if (contentType.contains(MediaType.APPLICATION_OCTET_STREAM)) {
        log.info("Processing OCTET STREAM output of type {}", contentType);
        apiOperation.getOutput().getItems().forEach((variable, attribute) ->
          Optional.ofNullable(attribute.getProperty())
            .ifPresent(value -> apiOperation.getRuntimeVariables().put(variable, apiResponse.getBody())));
      } else if (contentType.contains(MediaType.TEXT_PLAIN)) {
        log.info("Processing PLAIN TEXT output of type {}", contentType);
        apiOperation.getOutput().getItems().forEach((variable, attribute) ->
          Optional.ofNullable(attribute.getProperty())
            .ifPresent(value -> apiOperation.getRuntimeVariables().put(variable, apiResponse.getBody())));
      } else if (contentType.contains(BobConstants.APPLICATION_X_ND_JSON)) {
        log.info("Processing x-ndjson output of type {}", contentType);
        apiOperation.getOutput().getItems().forEach((variable, attribute) ->
          Optional.ofNullable(attribute.getProperty())
            .ifPresent(value -> apiOperation.getRuntimeVariables().put(variable, apiResponse.getBody())));
      }else {
        log.info("Processing JSON output of type {}", contentType);
        processingJsonResponse(responseJson, apiResponse, apiOperation);
      }
    }

  }

  /**
   * Processes the XML response.
   *
   * @param responseJson the response JSON
   * @param apiResponse  the API response body
   * @param apiOperation the API operation
   */
  private static void processingXmlResponse(AtomicReference<DocumentContext> responseJson, ApiResponseBody apiResponse,
                                            ApiOperation apiOperation) {

    try {
      if (ObjectUtils.isNotEmpty(apiResponse.getBody())) {
        responseJson.set(parse(apiResponse.getBody()));
      }
    } catch (InvalidJsonException parseException) {
      throw new NonRetryableException(String.format("Error parsing xml response body %s ", apiResponse.getBody()),
        parseException);
    }
    apiOperation.getOutput().getItems()
      .forEach((variable, attribute) -> Optional.ofNullable(attribute.getProperty()).map((String path) -> {
        if ("_$".equals(path)) {
          path = "$";
        } else if (path.startsWith("ROOT.")) {
          path = BobConstants.ROOT_PATTERN.matcher(path).replaceFirst("$.root.");
        } else {
          path = "$." + path;
        }
        return path.replace("#", ".");
      }).map(responseJson.get()::read).ifPresent(value -> apiOperation.getRuntimeVariables().put(variable, value)));
  }

  /**
   * Processes the JSON response.
   *
   * @param responseJson the response JSON
   * @param apiResponse  the API response body
   * @param apiOperation the API operation
   */
  private static void processingJsonResponse(AtomicReference<DocumentContext> responseJson, ApiResponseBody apiResponse,
                                             ApiOperation apiOperation) {

    parseJsonResponseBody(responseJson, apiResponse);

    processOutputVariables(responseJson, apiOperation);
  }

  private static void parseJsonResponseBody(AtomicReference<DocumentContext> responseJson, ApiResponseBody apiResponse) {
    try {
      if (ObjectUtils.isNotEmpty(apiResponse.getBody())) {
        responseJson.set(parse(apiResponse.getBody()));
      }
    } catch (InvalidJsonException parseException) {
      throw new NonRetryableException(
        String.format("Error parsing json response body : %s", apiResponse.getBody()), parseException
      );
    }
  }

  private static void processOutputVariables(AtomicReference<DocumentContext> responseJson, ApiOperation apiOperation) {
    apiOperation.getOutput().getItems()
      .forEach((variable, attribute) ->
        Optional.ofNullable(attribute.getProperty())
          .map(ApiResponseCallBack::sanitizeJsonPath)
          .map(responseJson.get()::read)
          .ifPresent(value -> setRuntimeVariable(apiOperation, variable, value))
      );
  }

  private static String sanitizeJsonPath(String path) {
    if ("_$".equals(path)) {
      return "$";
    }
    return path.replace("#", ".");
  }

  private static void setRuntimeVariable(ApiOperation apiOperation, String variable, Object value) {
    if (value instanceof List<?>) {
      try {
        apiOperation.getRuntimeVariables().put(variable, Config.OBJECT_MAPPER.writeValueAsString(value));
      } catch (JsonProcessingException e) {
        throw new NonRetryableException("Error serializing JSON value", e);
      }
    } else {
      apiOperation.getRuntimeVariables().put(variable, value);
    }
  }


}
