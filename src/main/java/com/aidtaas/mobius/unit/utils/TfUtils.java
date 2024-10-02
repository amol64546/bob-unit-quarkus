
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.config.URLResolver;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;

/**
 * This class represents a Terraform utility.
 * It is annotated with @Slf4j, @ApplicationScoped, and @RequiredArgsConstructor.
 * These annotations provide a logger, specify that the utility is application-scoped,
 * and generate a constructor that initializes the final fields.
 * The utility includes methods to send data to Terraform.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TfUtils {

  private final ConfigProperties config;

  private final DynamicRestClient restService;

  private final URLResolver urlResolver;

  /**
   * Sends data to Terraform.
   *
   * @param schemaDTO the API metering DTO
   * @param auth      the authorization
   */
  public void sendToTF(Object schemaDTO, String auth, String schemaId) {

    Map<String, String> pathParams = Map.of(BobConstants.SCHEMA_ID, schemaId);
    String finalUrl = urlResolver.constructUrl(config.tfEntityIngestionUrl(), null, pathParams);

    sendToTF(finalUrl, schemaDTO, auth);
  }

  /**
   * Sends data to Terraform.
   *
   * @param uri  the URI
   * @param body the body
   * @param auth the authorization
   */
  private void sendToTF(String uri, Object body, String auth) {
    log.info("POST request to TF URI {}", uri);
    try {

      List<Object> entityList = new ArrayList<>();
      entityList.add(body);
      Map<String, String> headers = new HashMap<>();
      headers.put(AUTHORIZATION, auth);
      ApiResponseBody response = restService.makeApiCall(uri, entityList, "POST", headers);
      log.info("Response from TF {}", response.getStatusCodeValue());
      if (response.getStatusCodeValue() == HttpURLConnection.HTTP_CREATED) {
        log.info("Posted the metering info to TF");
      } else {
        log.info("Unable to post the metering info to TF");
      }
    } catch (Exception e) {
      log.error("Error in sending to TF: {}", e.getMessage());
    }
  }
}
