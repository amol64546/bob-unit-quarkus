
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;


public enum SourceType {

  STATIC("STATIC"), GLOBALS("GLOBALS"), EXTERNALS("EXTERNALS"), PROPERTY("PROPERTY"), GROUP("GROUP"), SCRIPT("SCRIPT");

  private String value;

  SourceType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
