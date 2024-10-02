
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ScriptSource {
  GIT("GIT"),
  HTTP("HTTP"),
  GITEA("GITEA"),
  PIPELINE("PIPELINE");

  private String value;

  ScriptSource(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
