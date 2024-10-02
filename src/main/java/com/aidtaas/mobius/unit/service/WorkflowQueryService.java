
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.service;


import jakarta.ws.rs.core.HttpHeaders;

import java.util.Map;

public interface WorkflowQueryService {

  Long getSuspendedProcDefCount(String wfId, Integer deployedVersion, HttpHeaders httpHeaders);

  Map<String, Long> getActivityDataCount(String workflowID, String processInstanceId, String auth,
                                         HttpHeaders headers);

}
