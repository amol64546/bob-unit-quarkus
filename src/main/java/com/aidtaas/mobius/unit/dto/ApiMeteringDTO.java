
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiMeteringDTO extends BaseApiMeteringDTO {

  private static final long serialVersionUID = 1L;

  private String productOwnerName;
  private String productName;
  private String executorTenantId;
  private String executorTenantName;
  private String tenantId;
  private String productId;
  private String productOwner;
  private String workflowProcessInstanceId;
  private String workflowId;
  private String apiMethod;
  private String apiProduct;
  private Object apiBody;
  private Object apiResponse;
  private long apiResponseContentSize;
  private long apiResponseBodySize;
  private long apiRequestContentSize;
  private long apiRequestBodySize;
  private int responseStatus;
  private String tenantType;
  private long fileSizeInBytes;

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }
}
