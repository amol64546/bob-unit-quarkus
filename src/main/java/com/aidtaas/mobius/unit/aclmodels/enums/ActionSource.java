
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.aclmodels.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionSource {

  HUMAN_UI_LOGIN("HUMAN_UI_LOGIN"), HUMAN_UI_LOGOUT("HUMAN_UI_LOGOUT"),
  HUMAN_UI_PAGE_REDIRECT("HUMAN_UI_PAGE_REDIRECT"), HUMAN_API("HUMAN_API"), SYSTEM_API("SYSTEM_API"),
  SYSTEM_SCHEDULE("SYSTEM_SCHEDULE"), SYSTEM_CONSUMER("SYSTEM_CONSUMER"), AI("AI");

  private String value;

  ActionSource(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

}
