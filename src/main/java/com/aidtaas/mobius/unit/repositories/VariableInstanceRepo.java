
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.repositories;


import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.constants.SqlQueries;
import com.aidtaas.mobius.unit.dto.DataCountDTO;
import com.aidtaas.mobius.unit.exception.DataCountRetrievalException;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ApplicationScoped
public class VariableInstanceRepo {

  private final AgroalDataSource historyDataSource;

  public VariableInstanceRepo(@DataSource(BobConstants.HISTORY) AgroalDataSource historyDataSource) {
    this.historyDataSource = historyDataSource;
  }

  /**
   * Find data count by process instance id and variable name data count dto.
   *
   * @param processInstanceId the process instance id
   * @param outputVariable    the output variable
   * @return the data count dto
   */
  @Transactional
  public DataCountDTO findDataCountByProcessInstanceIdAndVariableName(String processInstanceId,
                                                                      String outputVariable) {
    DataCountDTO dataCountDTO = null;
    try (var con = historyDataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(String.format(SqlQueries.QUERY_GET_DATA_COUNT,
           processInstanceId, outputVariable))) {
      // Execute the query
      try (ResultSet rs = ps.executeQuery()) {
        // Process the result set
        if (rs.next()) {
          // Retrieve the values from the result set
          var name = rs.getString(BobConstants.NAME);
          var text2 = rs.getString(BobConstants.TEXT_2);
          var byteArrayId = rs.getString(BobConstants.BYTEARRAY_ID);

          // Do something with the retrieved values
          // For example, you can construct your DTO here
          dataCountDTO = new DataCountDTO(name, text2, byteArrayId);
        }
      }
    } catch (SQLException e) {
      throw new DataCountRetrievalException(String.format("Error retrieving data count for " +
        "process instance ID: %s and variable name: %s", processInstanceId, outputVariable), e);
    }
    return dataCountDTO;
  }


}
