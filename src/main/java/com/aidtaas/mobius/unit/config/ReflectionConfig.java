
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import com.aidtaas.mobius.unit.dto.DeploymentProcessDefinition;
import com.aidtaas.mobius.unit.dto.DeploymentResponse;
import com.aidtaas.mobius.unit.dto.Field;
import com.aidtaas.mobius.unit.dto.InputAttribute;
import com.aidtaas.mobius.unit.dto.Link;
import com.aidtaas.mobius.unit.dto.OutputAttribute;
import com.aidtaas.mobius.unit.enums.SourceType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.auth.NTLMEngine;
import org.apache.http.impl.client.CloseableHttpClient;
import org.camunda.bpm.client.impl.EngineRestExceptionDto;
import org.camunda.bpm.client.impl.RequestDto;
import org.camunda.bpm.client.impl.RequestExecutor;
import org.camunda.bpm.client.task.impl.ExternalTaskImpl;
import org.camunda.bpm.client.task.impl.dto.BpmnErrorRequestDto;
import org.camunda.bpm.client.task.impl.dto.CompleteRequestDto;
import org.camunda.bpm.client.task.impl.dto.ExtendLockRequestDto;
import org.camunda.bpm.client.task.impl.dto.FailureRequestDto;
import org.camunda.bpm.client.task.impl.dto.LockRequestDto;
import org.camunda.bpm.client.task.impl.dto.SetVariablesRequestDto;
import org.camunda.bpm.client.topic.impl.dto.FetchAndLockRequestDto;
import org.camunda.bpm.client.topic.impl.dto.TopicRequestDto;
import org.camunda.bpm.client.variable.impl.TypedValueField;
import org.camunda.bpm.client.variable.impl.VariableValue;
/**
 * This class is responsible for registering certain classes for reflection.
 * It is annotated with @RegisterForReflection, which means that
 * these classes will be registered for reflection at runtime.
 * This is necessary for some frameworks and libraries that use reflection to inspect classes at runtime.
 */
@RegisterForReflection(targets = {RequestExecutor.class, EngineRestExceptionDto.class, ExternalTaskImpl.class,
  TypedValueField.class, VariableValue.class, Field.class, InputAttribute.class, SourceType.class, RequestDto.class,
  OutputAttribute.class, LockRequestDto.class, FetchAndLockRequestDto.class, TopicRequestDto.class,
  SetVariablesRequestDto.class, CompleteRequestDto.class, ExtendLockRequestDto.class, BpmnErrorRequestDto.class,
  FailureRequestDto.class, NTLMEngine.class, HttpClient.class,
  CloseableHttpClient.class, DeploymentResponse.class,
  Link.class, DeploymentProcessDefinition.class})
public class ReflectionConfig {

}
