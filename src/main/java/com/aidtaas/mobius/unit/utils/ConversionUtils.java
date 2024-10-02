
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.enums.VariableType;
import com.aidtaas.mobius.unit.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a conversion utility.
 * It is annotated with @Slf4j and @RequiredArgsConstructor.
 * These annotations provide a logger and generate a constructor that initializes the final fields.
 * The utility includes methods to convert an object, type cast an object, convert a string, and convert to JSON.
 */
@Slf4j
public final class ConversionUtils {

  private static final Map<VariableType, UnaryOperator<Object>> typeConversionMap =
    new EnumMap<>(VariableType.class);

  private ConversionUtils() {
  }

  static {
    typeConversionMap.put(VariableType.OBJECT, value -> value);
    typeConversionMap.put(VariableType.STRING, Object::toString);
    typeConversionMap.put(VariableType.ARRAY, value -> convertToArray(value.toString()));
    typeConversionMap.put(VariableType.INTEGER, value -> Integer.valueOf(value.toString()));
    typeConversionMap.put(VariableType.NUMBER, value -> Double.valueOf(value.toString()));
    typeConversionMap.put(VariableType.DOUBLE, value -> Double.valueOf(value.toString()));
    typeConversionMap.put(VariableType.BOOLEAN, value -> Boolean.valueOf(value.toString()));
    typeConversionMap.put(VariableType.FILE, value -> value);
  }

  /**
   * Type casts an object to a specified class.
   *
   * @param value the value
   * @param clazz the class
   * @param <T>   the type
   * @return the type-casted object
   */
  @SneakyThrows
  public static <T> T typeCast(Object value, Class<?> clazz) {
    VariableType type = VariableType.valueOf(clazz);

    return typeCastHelper(type, value);

  }

  /**
   * Type casts a helper.
   *
   * @param type  the type
   * @param value the value
   * @param <T>   the type
   * @return the type-casted object
   */
  public static <T> T typeCastHelper(VariableType type, Object value) {
    if (ObjectUtils.isEmpty(type)) {
      throw new ValidationException("Type undefined!");
    }

    if (ObjectUtils.isEmpty(value)) {
      return null;
    }

    UnaryOperator<Object> converter = typeConversionMap.get(type);
    if (ObjectUtils.isEmpty(converter)) {
      throw new ValidationException(String.format("No converter found for type: %s", type));
    }

    try {
      return (T) converter.apply(value);
    } catch (IllegalArgumentException conversionException) {
      throw new ValidationException(
        String.format("Error converting %s to an object of type %s ", value, type), conversionException);
    }
  }

  /**
   * Converts a string to an array.
   *
   * @param textValue the text value
   * @return the array
   */
  private static String[] convertToArray(String textValue) {
    if (StringUtils.isEmpty(textValue)) {
      return new String[0];
    }
    return textValue.substring(1, textValue.length() - 1).split(",");
  }

  /**
   * Converts an object to a JSON node.
   *
   * @param data the data
   * @return the JSON node
   */
  @SneakyThrows
  public static JsonNode convertToJson(Object data) {

    if (ObjectUtils.isNotEmpty(data)) {

      return Config.OBJECT_MAPPER.readTree(data.toString());
    } else {
      return null;
    }
  }
}
