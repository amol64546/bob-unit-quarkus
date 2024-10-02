
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DataType {
  INTEGER("INTEGER"),
  DOUBLE("DOUBLE"),
  SHORT("SHORT"),
  LONG("LONG"),
  STRING("STRING"),
  SPIN_JSON_NODE("SPIN_JSON_NODE"),
  DEFAULT("DEFAULT");

  private String value;

  DataType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
