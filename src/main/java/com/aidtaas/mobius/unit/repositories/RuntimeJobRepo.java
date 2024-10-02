
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
public class RuntimeJobRepo {

  private final EntityManager entityManager;

  /**
   * Suspend running jobs by tenant id.
   *
   * @param tenantId the tenant id
   */
  @Transactional
  public void suspendRunningJobsByTenantId(String tenantId) {
    var sql = String.format(SqlQueries.SUSPEND_JOB_QUERY, tenantId);
    entityManager.createNativeQuery(sql).executeUpdate();
  }

  /**
   * Activate running jobs by tenant id.
   *
   * @param tenantId the tenant id
   */
  @Transactional
  public void activateRunningJobsByTenantId(String tenantId) {
    var sql = String.format(SqlQueries.ACTIVATE_JOB_QUERY, tenantId);
    entityManager.createNativeQuery(sql).executeUpdate();
  }

}
