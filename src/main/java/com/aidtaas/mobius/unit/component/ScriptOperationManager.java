
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.component;

import com.aidtaas.mobius.unit.dto.ScriptOperation;
import com.jayway.jsonpath.DocumentContext;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION_GLOBAL;

/**
 * The type Api operation manager.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ScriptOperationManager {

  private final ApiOperationManager apiOperationManager;

  /**
   * Retrieve rest api info.
   *
   * @param scriptOperation
   */
  public void retrieveScriptInfo(ScriptOperation scriptOperation) {

    String auth = scriptOperation.getExternalTask().getVariable(AUTHORIZATION_GLOBAL);

    String productId = scriptOperation.getInput().getComponentId();
    String productMasterConfigId = scriptOperation.getInput().getProductMasterConfigId();

    scriptOperation.setProductId(productId);
    scriptOperation.setProductMasterConfigId(productMasterConfigId);

    DocumentContext productJson = apiOperationManager
      .retrieveProductConfigForSpecificApis(productId, productMasterConfigId, null, auth);
    scriptOperation.setProductJson(productJson);
  }
}
