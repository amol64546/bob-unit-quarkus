
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.repositories;

import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.constants.SqlQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aidtaas.mobius.unit.constants.BobConstants.TENANT_ID;


@ApplicationScoped
@RequiredArgsConstructor
public class RunningWorkflowInstanceRepo {

  private final EntityManager entityManager;

  /**
   * Suspend workflow instance by tenant id.
   *
   * @param tenantId the tenant id
   */
  @Transactional
  public void suspendWorkflowInstanceByTenantId(String tenantId) {
    var sql = String.format(SqlQueries.WORKFLOW_INSTANCE_BY_STATE_QUERY, BobConstants.SUSPEND_STATE);
    entityManager.createNativeQuery(sql).setParameter(TENANT_ID, tenantId).executeUpdate();
  }

  /**
   * Activate workflow instance by tenant id.
   *
   * @param tenantId the tenant id
   */
  @Transactional
  public void activateWorkflowInstanceByTenantId(String tenantId) {
    var sql = String.format(SqlQueries.WORKFLOW_INSTANCE_BY_STATE_QUERY, BobConstants.ACTIVE_STATE);
    entityManager.createNativeQuery(sql).setParameter(TENANT_ID, tenantId).executeUpdate();
  }

  /**
   * Get Suspended workflow process Definitions by process definition id.
   *
   * @param procDefId the proc def id
   */
  public Long getSuspendedProcDef(String procDefId) {
    String sql = SqlQueries.GET_SUSPENDED_PROC_DEF_QUERY;
    return (Long) entityManager.createNativeQuery(sql).setParameter
      (BobConstants.PROC_DEF_ID, procDefId).getSingleResult();
  }

  /**
   * Get Suspended workflow process Definitions count for each workflow.
   *
   * @param wfIds the proc def id
   */
  public Map<String, String> getSuspendedProcessDefinitionCountForEachWfs(List<String> wfIds) {
    // Generate comma-separated placeholders for the IN clause
    String placeholders = wfIds.stream().map(wfId -> "?").collect(Collectors.joining(", "));
    // SQL query with dynamic IN clause
    var sql = String.format(SqlQueries.GET_SUSPENDED_PROC_DEF_COUNT_QUERY, placeholders);

    // Create the native query
    var query = entityManager.createNativeQuery(sql);

    // Set parameters for the IN clause
    for (var i = 0; i < wfIds.size(); i++) {
      query.setParameter(i + 1, wfIds.get(i));
    }

    // Execute the query
    List<Object[]> results = query.getResultList();

    // Convert the results to a Map<String, String>
    return results.stream()
      .collect(Collectors.toMap(
        // Key extractor function
        row -> (String) row[0],
        // Value extractor function
        row -> row[1].toString()
      ));
  }

}
