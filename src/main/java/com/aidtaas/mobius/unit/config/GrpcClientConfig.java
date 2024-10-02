
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.grpc.MeteringServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * This class is responsible for configuring the gRPC client.
 * It is annotated with @ApplicationScoped, meaning that a single instance will be
 * created and shared across the application.
 */
@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class GrpcClientConfig {

  private final ConfigProperties config;

  /**
   * Produces a blocking stub for the MeteringService.
   * This stub can be used to make synchronous gRPC calls to the MeteringService.
   *
   * @return a MeteringServiceBlockingStub instance
   */
  @Produces
  public MeteringServiceGrpc.MeteringServiceBlockingStub meteringServiceBlockingStub() {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(config.grpcServerAddress(), config.grpcServerPort())
            .usePlaintext()
            .defaultLoadBalancingPolicy("pick_first")
            .defaultServiceConfig(getConfig())
            .enableRetry()
            .build();
//    forTarget(config.grpcServerAddress())
//      .usePlaintext()
//      .build()
    log.info("-----grpc address : {}", config.grpcServerAddress());
    log.info("-----channel : {}", channel);
    return MeteringServiceGrpc.newBlockingStub(channel);
  }

  private static Map<String, Object> getConfig() {
    Map<String, Object> name = new HashMap<>();
    name.put("service", "MeteringService");
    name.put("method", "Grpc");

    Map<String, Object> retryPolicy = new HashMap<>();
    retryPolicy.put("maxAttempts", BobConstants.MAX_ATTEMPTS);
    retryPolicy.put("initialBackoff", "0.1s");
    retryPolicy.put("maxBackoff", "1s");
    retryPolicy.put("backoffMultiplier", BobConstants.BACK_OFF_MULTIPLIER);
    retryPolicy.put("retryableStatusCodes", singletonList("UNAVAILABLE"));

    Map<String, Object> methodConfig = new HashMap<>();
    methodConfig.put("name", singletonList(name));
//        methodConfig.put("timeout", "5s")
    methodConfig.put("retryPolicy", retryPolicy);
    methodConfig.put("wait_for_ready", true);

    Map<String, Object> configMap = new HashMap<>();
    configMap.put("methodConfig", singletonList(methodConfig));

    return configMap;
  }

}
