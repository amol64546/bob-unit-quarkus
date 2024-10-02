
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.constants;

public final class SqlQueries {


  public static final String QUERY_GET_DATA_COUNT = "SELECT v.name_, v.text2_, v.bytearray_id_ " +
    "FROM act_hi_varinst v WHERE v.proc_inst_id_ = '%s' AND v.name_ = '%s'";

  public static final String SUSPEND_WF_BY_ID_QUERY = "UPDATE act_re_procdef SET SUSPENSION_STATE_ = 2 " +
    "WHERE TENANT_ID_ = '%s'";

  public static final String ACTIVATE_WF_BY_ID_QUERY = "UPDATE act_re_procdef SET SUSPENSION_STATE_ = 1 " +
    "WHERE TENANT_ID_ = '%s'";

  public static final String SUSPEND_JOB_QUERY = "UPDATE act_ru_job SET SUSPENSION_STATE_ = 2 WHERE TENANT_ID_ = '%s'";

  public static final String ACTIVATE_JOB_QUERY = "UPDATE act_ru_job SET SUSPENSION_STATE_ = 1 WHERE TENANT_ID_ = '%s'";

  public static final String WORKFLOW_INSTANCE_BY_STATE_QUERY =
    "UPDATE act_ru_execution SET SUSPENSION_STATE_ = %d WHERE TENANT_ID_ = :tenantId";

  public static final String GET_SUSPENDED_PROC_DEF_QUERY =
    "SELECT COUNT(proc_def_id_) FROM act_ru_execution e " +
      "WHERE :procDefId = SUBSTRING(e.proc_def_id_ FROM '([^:]*:[^:]*):')";

  public static final String GET_SUSPENDED_PROC_DEF_COUNT_QUERY =
    "SELECT " +
      "SUBSTRING(proc_def_id_ FROM 'GaianWorkflows([0-9]+)') AS wfId, " +
      "COUNT(*) AS suspension_count " +
      "FROM act_ru_execution " +
      "WHERE suspension_state_ = 2 " +
      "AND proc_def_id_ ILIKE 'GaianWorkflows%%' " +
      "AND SUBSTRING(proc_def_id_ FROM 'GaianWorkflows([0-9]+)') IN (%s) " +
      "GROUP BY wfId";

  public static final String BYTES_BY_BYTEARRAY_ID_QUERY = "SELECT bytes_ FROM act_ge_bytearray WHERE id_ = '%s'";

  private SqlQueries() {
  }
}
