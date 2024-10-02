
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.aclmodels.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RequesterType {
  TENANT("TENANT"), CONSUMER("CONSUMER"), TENANT_USER("TENANT_USER");

  private String value;

  RequesterType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

}
