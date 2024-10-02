
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.aclmodels.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {

  CREATE("CREATE"), UPDATE("UPDATE"), DELETE("DELETE"), EVAL("EVAL"), ERROR("ERROR"), GET("GET"), EXPIRY("EXPIRY"),
  GRANTED("GRANTED"), REVOKED("REVOKED");

  private String value;

  ActionType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

}
