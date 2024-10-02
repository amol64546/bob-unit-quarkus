
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.component;

import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.config.URLResolver;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiOperation;
import com.aidtaas.mobius.unit.dto.ApiResponse;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.client.task.ExternalTask;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;
import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION_GLOBAL;
import static com.aidtaas.mobius.unit.constants.BobConstants.BUYER_ID;

/**
 * This class is responsible for performing API operations.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RestApiOperator {

  private final DynamicRestClient dynamicRestClient;

  private final ApiResponseCallBack apiResponseCallBack;

  private final URLResolver urlResolver;

  private final ResolveData resolveData;

  /**
   * Performs the API operation.
   *
   * @param restApiOperation the REST API operation
   * @param externalTask     the external task
   */
  public void performOperation(ApiOperation restApiOperation, ExternalTask externalTask) {

    log.info("-------start-------Thread name and id at RestApiOperator, {} {}", Thread.currentThread().getName(),
      Thread.currentThread().threadId());

    var restApi = resolveData.resolveAllApiData(restApiOperation);

    log.info("Starting web client api call for service task brick: {}", restApi);

    String httpMethod = restApi.getHttpMethod();

    Map<String, String> headers = restApi.getHeaders();
    headers.put(BobConstants.PRODUCT_ID, restApiOperation.getProductId());
    headers.put(BUYER_ID, restApiOperation.getExecutorTenantId());

    if (headers.containsKey(AUTHORIZATION) && StringUtils.isEmpty(headers.get(AUTHORIZATION))) {
      headers.put(AUTHORIZATION, externalTask.getVariable(AUTHORIZATION_GLOBAL).toString());
    }

    Map<String, String> pathParams = restApi.getPathParams();

    Map<String, String> queryParams = restApi.getQueryParams();

    Object body = restApi.getBody();

    String endpoint = restApi.getUrl();
    String resolvedUrl = urlResolver.constructUrl(endpoint, queryParams, pathParams);

    log.info("Resolved URL: {}, Body: {}, headers: {}", resolvedUrl, body, headers);

    ApiResponseBody response;

    if (headers.containsKey(BobConstants.CONTENT_TYPE)
      && (headers.get(BobConstants.CONTENT_TYPE).equalsIgnoreCase(BobConstants.MULTIPART_FORM_DATA) ||
      headers.get(BobConstants.CONTENT_TYPE).equalsIgnoreCase(BobConstants.APPLICATION_X_WWW_FORM_URLENCODED))) {
      response = dynamicRestClient.makeMultipartApiCall(resolvedUrl, (Map<String, Object>) body, httpMethod, headers);
    } else {
      if (headers.containsKey(BobConstants.CONTENT_TYPE)
        && (headers.get(BobConstants.CONTENT_TYPE).equalsIgnoreCase(BobConstants.APPLICATION_JSON))) {
        headers.remove(BobConstants.CONTENT_TYPE);
      }
      response = dynamicRestClient.makeApiCall(resolvedUrl, body, httpMethod, headers);
    }

    ApiResponse apiResponse = ApiResponse.builder()
      .executionId(externalTask.getId()).apiResponseBody(response).apiOperation(restApiOperation)
      .httpMethod(httpMethod).resolvedUrl(resolvedUrl).headers(headers).body(body).build();

    apiResponseCallBack.sendMsgBack(externalTask, apiResponse);
  }
}
