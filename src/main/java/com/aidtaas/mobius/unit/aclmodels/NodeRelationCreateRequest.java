
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.aclmodels;

import com.aidtaas.mobius.unit.aclmodels.enums.NodeType;
import com.aidtaas.mobius.unit.aclmodels.enums.RequesterType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeRelationCreateRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private RequesterType requesterType;
  private String requesterId;
  private NodeType fromNode;
  private String fromNodeId;
  private NodeType toNode;
  private String toNodeId;
  private String universeId;
  private String fromNodeName;
  private boolean permissionRequired;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }
}
