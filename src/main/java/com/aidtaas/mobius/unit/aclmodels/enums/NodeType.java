
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.aclmodels.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NodeType {

  // Pascal Intelligence
  DATAVERSE("DATAVERSE"), COHORTS("COHORTS"), BIGQUERY("BIGQUERY"), CONTEXT("CONTEXT"), ENTITY(
    "ENTITY"),

  // Monet
  APPLETS("APPLETS"), EXPERIENCES("EXPERIENCES"), WIDGETS("WIDGETS"), PLUGINS("PLUGINS"),

  // BoltzmannsBot
  BRICKS("BRICKS"), WORKFLOW("WORKFLOW"), PACKAGES("PACKAGES"),

  // Holacracy
  PRODUCT("PRODUCT"), TENANT("TENANT"), ALLIANCE("ALLIANCE"), RATECARD("RATECARD"),

  // Remaining old enums
  TEMPLATE("TEMPLATE"), INGESTION("INGESTION"), ACCOUNT("ACCOUNT"), ORGANISATION(
    "ORGANISATION"), PLATFORM("PLATFORM"), NEGOTIATION("NEGOTIATION"), SUBALLIANCE(
    "SUBALLIANCE"), WALLETUSERDETAILS("WALLETUSERDETAILS"), LEDGER("LEDGER"), CONTENT("CONTENT");


  private String value;

  NodeType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
