
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.grpc;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class represents a gRPC client.
 * It is annotated with @Slf4j, @ApplicationScoped, and @RequiredArgsConstructor.
 * These annotations provide a logger, specify that the client is application-scoped,
 * and generate a constructor that initializes the final fields.
 * The client includes a field for the metering service stub and a method to send a message.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class GrpcClient {

  private final MeteringServiceGrpc.MeteringServiceBlockingStub stub;

  /**
   * Sends a message to the server.
   * It logs the response from the server.
   *
   * @param request the request to send
   */
  public void sendMessage(ApiMetering request) {
    var serverOutput = stub.grpc(request);
    log.info("grpc response: {}", serverOutput);
  }

}
