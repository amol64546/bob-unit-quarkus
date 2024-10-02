
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.controller.impl;

import com.aidtaas.mobius.unit.controller.MessageListenerController;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.service.MessageListenerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MessageListenerControllerImpl implements MessageListenerController {

  private final MessageListenerService messageListenerService;

  /**
   * Triggers the message event.
   *
   * @param workflowEvent The workflow event.
   * @param httpHeaders   The http headers.
   * @return The response.
   */
  @Override
  public ApiResponseBody triggerMessageEvent(Object workflowEvent, @Context HttpHeaders httpHeaders) {
    log.info("POST:/v1.0/trigger/message-listener");
    return messageListenerService.triggerMessageEvent(workflowEvent, httpHeaders);
  }
}
