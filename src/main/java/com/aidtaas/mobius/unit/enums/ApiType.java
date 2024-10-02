
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApiType {

  REST("REST"), SOAP("SOAP");

  private String value;

  ApiType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
