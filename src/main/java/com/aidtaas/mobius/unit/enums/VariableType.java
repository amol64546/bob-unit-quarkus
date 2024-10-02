
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import org.camunda.bpm.engine.variable.value.FileValue;

import static java.util.Arrays.stream;

/**
 * This enum represents the variable type.
 * It includes fields for array, string, integer, double, number, boolean, object, and file.
 * It is annotated with @AllArgsConstructor.
 * This annotation provides a constructor with all arguments.
 */
public enum VariableType {

  ARRAY("ARRAY", Arrays.class), STRING("STRING", String.class), INTEGER("INTEGER", Integer.class),
  DOUBLE("DOUBLE", Double.class), NUMBER("NUMBER", Number.class), // Deprecated
  BOOLEAN("BOOLEAN", Boolean.class), OBJECT("OBJECT", Object.class), FILE("FILE", FileValue.class);

  private String value;

  private Class<?> clazz;

  VariableType(String value, Class<?> clazz) {
    this.value = value;
    this.clazz = clazz;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  /**
   * This method returns the variable type for the given class.
   *
   * @param type the class type
   * @return the variable type
   */
  public static <T> VariableType valueOf(Class<T> type) {
    return stream(values()).parallel().filter(value -> value.clazz.equals(type)).findAny()
      .orElseThrow(() -> new UnsupportedOperationException(type + " class is not supported currently"));
  }
}
