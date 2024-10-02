
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.service.impl;

import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.service.MessageListenerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MessageListenerServiceImpl implements MessageListenerService {

  private final ConfigProperties engineRestConfig;
  private final DynamicRestClient dynamicRestClient;

  /**
   * Triggers the message event.
   *
   * @param workflowEvent the workflow event
   * @param httpHeaders   the http headers
   * @return the response
   */
  @Override
  public ApiResponseBody triggerMessageEvent(Object workflowEvent, HttpHeaders httpHeaders) {

    var auth = httpHeaders.getHeaderString(AUTHORIZATION);
    String finalUrl = engineRestConfig.engineRestUrl() + "/message";
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(AUTHORIZATION, auth);

    return dynamicRestClient.makeApiCall(finalUrl, workflowEvent, BobConstants.POST, headers);
  }
}
