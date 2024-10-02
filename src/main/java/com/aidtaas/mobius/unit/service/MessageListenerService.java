
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.service;

import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import jakarta.ws.rs.core.HttpHeaders;

public interface MessageListenerService {
  ApiResponseBody triggerMessageEvent(Object workflowEvent, HttpHeaders httpHeaders);
}
