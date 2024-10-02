
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.repositories;

import com.aidtaas.mobius.unit.constants.SqlQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkflowDefinitionRepo {

  private final EntityManager entityManager;

  /**
   * Suspend workflows by tenant id.
   *
   * @param tenantId the tenant id
   */
  @Transactional
  public void suspendWorkflowsByTenantId(String tenantId) {
    var sql = String.format(SqlQueries.SUSPEND_WF_BY_ID_QUERY, tenantId);
    entityManager.createNativeQuery(sql).executeUpdate();
  }

  /**
   * Activate workflows by tenant id.
   *
   * @param tenantId the tenant id
   */
  @Transactional
  public void activateWorkflowsByTenantId(String tenantId) {
    var sql = String.format(SqlQueries.ACTIVATE_WF_BY_ID_QUERY, tenantId);
    entityManager.createNativeQuery(sql).executeUpdate();
  }

}
