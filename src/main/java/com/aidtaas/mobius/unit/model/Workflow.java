
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.model;

import com.aidtaas.mobius.unit.aclmodels.enums.NodeType;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow implements Serializable {

  private static final long serialVersionUID = 1L;

  private String wfId;
  private Integer draftVersion;
  private String name;
  private String description;
  private String xml;
  private String deploymentId;
  @Builder.Default
  private String status = BobConstants.DRAFT;
  private Date createdOn;
  private String createdBy;
  private Date updatedOn;
  private String ownerId;
  private Integer deployedVersion;
  @Builder.Default
  private Map<String, Set<String>> tags = new HashMap<>();
  private String icon;
  private String thumbnail;
  @Builder.Default
  private Map<NodeType, List<String>> constructs = new EnumMap<>(NodeType.class);

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }

}
