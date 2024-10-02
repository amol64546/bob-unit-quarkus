
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides configuration constants and utilities for the application.
 * It is annotated with @ApplicationScoped, meaning that a single instance will be
 * created and shared across the application.
 */
@Slf4j
@ApplicationScoped
public final class Config {

  /**
   * ObjectMapper instance for converting between Java objects and JSON.
   * This instance is shared across the application.
   */
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private Config() {
  }

  @PostConstruct
  public void init() {
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    log.info("-----ObjectMapper Bean Created Successfully-----");
  }

}
