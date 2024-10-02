
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.dto;

import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.utils.CommonUtils;
import com.aidtaas.mobius.unit.utils.ConversionUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import static com.aidtaas.mobius.unit.constants.BobConstants.APP_ID_GLOBAL;
import static com.aidtaas.mobius.unit.constants.BobConstants.ENVIRONMENT_GLOBAL;
import static com.aidtaas.mobius.unit.constants.BobConstants.TENANT_ID_GLOBAL;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Operation implements Serializable {

  private static final long serialVersionUID = 1L;

  protected ExternalTask externalTask;
  protected ExternalTaskService externalTaskService;
  @Builder.Default
  protected Map<String, Object> runtimeVariables = new HashMap<>();
  protected String executorTenantId;
  protected String executorTenantName;
  protected String executorTenantType;
  protected String creatorId;
  protected Environment environment;
  protected String appId;
  protected long timeoutDuration;

  public Operation(ExternalTask externalTask, ExternalTaskService externalTaskService) {

    this.externalTask = externalTask;
    this.externalTaskService = externalTaskService;
    this.runtimeVariables = new HashMap<>();

    Optional.ofNullable(externalTask.getVariable(APP_ID_GLOBAL))
      .map(Object::toString).ifPresent(this::setAppId);
    Optional.ofNullable(externalTask.getVariable(TENANT_ID_GLOBAL))
      .map(Object::toString).ifPresent(this::setExecutorTenantId);
    Optional.ofNullable(externalTask.getVariable(TENANT_ID_GLOBAL))
      .map(Object::toString).ifPresent(this::setCreatorId);
    Optional.ofNullable(externalTask.getVariable(ENVIRONMENT_GLOBAL))
      .map(Object::toString).map(Environment::valueOf).ifPresent(this::setEnvironment);
  }

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


  /**
   * Gets a runtime variable.
   * It sanitizes the process value before returning it.
   *
   * @param variable the variable
   * @return the runtime variable
   */
  public Object getRunTimeVariable(String variable) {
    return CommonUtils.sanitizeProcessValue(
      externalTask.getVariableTyped(String.format(BobConstants.RUN_TIME_VARIABLE_FORMAT, variable)));
  }

  /**
   * Gets a casted runtime variable.
   * It sanitizes the process value and casts it to the specified type before returning it.
   *
   * @param variable the variable
   * @param type     the type
   * @return the casted runtime variable
   */
  public <T> T getCastedRunTimeVariable(String variable, Class<?> type) {
    return Optional.ofNullable(CommonUtils.sanitizeProcessValue(
        externalTask.getVariableTyped(String.format(BobConstants.RUN_TIME_VARIABLE_FORMAT, variable))))
      .map(value -> (T) ConversionUtils.typeCast(value, type)).orElse(null);
  }
}
