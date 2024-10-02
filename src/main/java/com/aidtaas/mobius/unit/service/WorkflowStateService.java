
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.service;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.CompletionStage;

public interface WorkflowStateService {
  CompletionStage<Response> suspendRunningJobsByTenantId(HttpHeaders httpHeaders);

  CompletionStage<Response> activateRunningJobsByTenantId(HttpHeaders httpHeaders);

  CompletionStage<Response> suspendWorkflowInstanceByTenantId(HttpHeaders httpHeaders);

  CompletionStage<Response> activateWorkflowInstanceByTenantId(HttpHeaders httpHeaders);

  CompletionStage<Response> suspendWorkflowsByTenantId(HttpHeaders httpHeaders);

  CompletionStage<Response> activateWorkflowsByTenantId(HttpHeaders httpHeaders);
}
