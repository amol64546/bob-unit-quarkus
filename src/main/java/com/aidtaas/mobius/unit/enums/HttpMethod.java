
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum HttpMethod {
  POST("POST"), GET("GET"), PUT("PUT"), DELETE("DELETE");

  private String value;

  HttpMethod(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
