
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.service;

import com.aidtaas.mobius.unit.aclmodels.enums.RequesterType;
import com.aidtaas.mobius.unit.enums.Environment;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;

public interface CamundaService {

  Response deployWorkflow(String id, String requesterId, RequesterType requesterType, String auth) throws IOException;

  Response triggerWorkflow(String id, String requesterId, RequesterType requesterType,
                           String bearerToken, Environment env, String appId, MultipartFormDataInput dataInput);


}
