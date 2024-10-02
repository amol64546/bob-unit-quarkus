
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentType {
  APPLICATION_JSON("APPLICATION_JSON"),
  APPLICATION_X_WWW_FORM_URLENCODED("APPLICATION_X_WWW_FORM_URLENCODED"),
  MULTIPART_FORM_DATA("MULTIPART_FORM_DATA"),
  APPLICATION_OCTET_STREAM("APPLICATION_OCTET_STREAM"),
  APPLICATION_XML("APPLICATION_XML"),
  TEXT_PLAIN("TEXT_PLAIN");

  private String value;

  ContentType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
