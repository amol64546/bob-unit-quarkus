
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.aclmodels.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionLogRequestStatus {

  SUCCESS("SUCCESS"), FAILED("FAILED");

  private String value;

  ActionLogRequestStatus(String value) {
    this.value = value;
  }

  @JsonValue
  private String value() {
    return value;
  }
}
