
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobStatusDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String tenantId;
  private String workflowProcessInstanceId;
  private String workflowInstanceName;
  private String state;
  private String message;
  private String workflowName;
  private String tenantName;
  private String version;
  @Builder.Default
  private List<String> activityNames = new ArrayList<>();
  @Builder.Default
  private List<String> schemaIds = new ArrayList<>();
  private boolean ingestionJob;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }

}
