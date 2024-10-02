
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Environment {

  TEST("TEST"), PROD("PROD");

  private String value;

  Environment(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
