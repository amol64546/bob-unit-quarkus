
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

@StaticInitSafe
@ConfigMapping(prefix = "bob.unit")
public interface ConfigProperties {

  String bobGetPipelineUrl();

  String aclActionLogSchemaId();

  String marketplaceMasterConfigUrl();

  String marketplaceAllianceUrl();

  String tfEntityIngestionUrl();

  String apiMeteringDtoSchemaId();

  String jobStatusDtoSchemaId();

  String infraDtoSchemaId();

  String userDtoSchemaId();

  String engineRestUrl();

  // API Worker configuration
  Integer apiAsyncResponseTimeout();

  Integer apiWorkerCount();

  Integer apiMaxTasks();

  String apiWorkerId();

  Long apiLockDuration();

  // Terraform Worker configuration
  Integer terraformAsyncResponseTimeout();

  Integer terraformWorkerCount();

  Integer terraformMaxTasks();

  String terraformWorkerId();

  Long terraformLockDuration();

  // ShellScript Worker configuration
  Integer shellScriptAsyncResponseTimeout();

  Integer shellScriptWorkerCount();

  Integer shellScriptMaxTasks();

  String shellScriptWorkerId();

  Long shellScriptLockDuration();

  // Ansible Worker configuration
  Integer ansibleAsyncResponseTimeout();

  Integer ansibleWorkerCount();

  Integer ansibleMaxTasks();

  String ansibleWorkerId();

  Long ansibleLockDuration();

  // Python Worker configuration
  Integer pythonAsyncResponseTimeout();

  Integer pythonWorkerCount();

  Integer pythonMaxTasks();

  String pythonWorkerId();

  Long pythonLockDuration();

  long backoffMultiplier();

  float maxBackoffDelay();

  long waitTime();

  boolean usePriority();

  Integer retryCount();

  Integer retryDelay();

  Integer restClientReadTimeout();

  Integer restClientConnectTimeout();

  Integer restClientMaxPoolSize();

  Integer restClientMaxPerRoute();

  String grpcServerAddress();

  Integer grpcServerPort();

  String serviceDomain();

  String aclCreateBulkNodeRelationUrl();

  String aclDeleteNodeRelationUrl();

  String serviceLatestWf();

  String serviceDeployedVersion();

  String serviceSaveDeployment();

  String serviceDeployedWf();

  String redisHost();

  String redisPassword();

  boolean redisPasswordEnable();

  boolean redisClusterEnable();

}
