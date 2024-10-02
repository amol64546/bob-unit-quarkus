
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.component;

import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.config.URLResolver;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiOperation;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.unit.exception.RetryableException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;
import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION_GLOBAL;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.lang.String.format;

/**
 * The type Api operation manager.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ApiOperationManager {

  private final DynamicRestClient restTemplate;

  private final URLResolver urlResolver;

  private final ConfigProperties config;

  /**
   * Retrieve rest api info.
   *
   * @param apiOperation
   */
  public void retrieveRestApiInfo(ApiOperation apiOperation) {

    String auth = apiOperation.getExternalTask().getVariable(AUTHORIZATION_GLOBAL);

    String productId = apiOperation.getInput().getComponentId();
    String productMasterConfigId = apiOperation.getInput().getProductMasterConfigId();

    var environment = apiOperation.getEnvironment();
    apiOperation.setProductId(productId);
    apiOperation.setProductMasterConfigId(productMasterConfigId);

    DocumentContext productJson;
    if (environment.getValue().equalsIgnoreCase(Environment.TEST.getValue())) {
      productJson = retrieveProductConfigForSpecificApis(productId, productMasterConfigId,
        apiOperation.getInput().getApiPath(), auth);
    } else {
      productJson = retrieveAllianceConfig(apiOperation.getCreatorId(),
        apiOperation.getAppId(), auth);
    }
    apiOperation.setProductJson(productJson);
  }

  /**
   * Retrieve alliance config document context.
   *
   * @param creatorId
   * @param appId
   * @param auth
   * @return
   */
  private DocumentContext retrieveAllianceConfig(String creatorId, String appId, String auth) {

    Map<String, String> pathParams = new HashMap<>();
    pathParams.put(BobConstants.BUYER_ID, creatorId);
    pathParams.put(BobConstants.APP_ID, appId);

    String configUrl = urlResolver.constructUrl(config.marketplaceAllianceUrl(), null, pathParams);

    String allianceJsonString;

    try {
      Map<String, String> headers = new HashMap<>();
      headers.put(AUTHORIZATION, auth);
      allianceJsonString = restTemplate.makeApiCall(configUrl, null, "GET", headers).getBody();
      if (ObjectUtils.isEmpty(allianceJsonString)) {
        throw new NonRetryableException("No alliance config available for this appId and creatorId");
      }
      log.info("Received alliance json for component {}", appId);

    } catch (RetryableException restException) {
      String errorMessage = format(
        "Api to retrieve the alliance config for product %s from %s :: %s ", appId,
        configUrl, restException.getMessage());
      log.error(errorMessage, restException);
      throw new RetryableException(
        "Failed to retrieve alliance || " + errorMessage + " || " + restException.getMessage(),
        restException);
    }

    try {
      return parse(allianceJsonString);

    } catch (InvalidJsonException parseException) {
      String errorMessage = format(
        "Error parsing the json of alliance for product %s retrieved from marketplace :: %s ", appId,
        allianceJsonString);
      log.error(errorMessage, parseException);
      throw new NonRetryableException(errorMessage, parseException);
    }
  }

  /**
   * Retrieve product config for specific apis document context.
   *
   * @param productId
   * @param productMasterConfigId
   * @param apiPath
   * @param auth
   * @return
   */
  public DocumentContext retrieveProductConfigForSpecificApis(String productId, String productMasterConfigId,
                                                               String apiPath, String auth) {

    log.info("Api path : {}", apiPath);
    Map<String, String> pathParams = Map.of("masterConfigId", productMasterConfigId);
    Map<String, String> queryParams = null;
    if(StringUtils.isNotEmpty(apiPath)) {
      queryParams = Map.of("interfaceName",
        URLEncoder.encode(Arrays.stream(apiPath.split("#")).findFirst().orElse(""), StandardCharsets.UTF_8));
    }
    String configUrl = urlResolver.constructUrl(config.marketplaceMasterConfigUrl(), queryParams, pathParams);
    log.info("ConfigUrl : {}", configUrl);
    String productJsonString;

    try {
      Map<String, String> headers = new HashMap<>();
      headers.put(AUTHORIZATION, auth);
      productJsonString = restTemplate.makeApiCall(configUrl, null, "GET", headers).getBody();
      if (ObjectUtils.isEmpty(productJsonString)) {
        throw new NonRetryableException("No product config available for this masterConfigId and productId");
      }
      log.info("Received product masterConfig json for component {}", productId);

    } catch (RetryableException restException) {
      String errorMessage = format(
        "Api to retrieve the masterConfig for product %s from %s :: %s ", productId,
        configUrl, restException.getMessage());
      throw new RetryableException(errorMessage + " || " + restException.getMessage(), restException);
    } catch (NonRetryableException exception) {
      String errorMessage = format(
        "Api to retrieve the masterConfig for product %s from %s :: %s ", productId,
        configUrl, exception.getMessage());
      throw new NonRetryableException(errorMessage + " || " + exception.getMessage(), exception);
    }

    try {
      return parse(productJsonString);
    } catch (InvalidJsonException parseException) {
      String errorMessage = format(
        "Error parsing the json of product masterConfig %s retrieved from marketplace :: %s ", productId,
        productJsonString);
      log.error(errorMessage, parseException);
      throw new NonRetryableException(errorMessage, parseException);
    }
  }
}
