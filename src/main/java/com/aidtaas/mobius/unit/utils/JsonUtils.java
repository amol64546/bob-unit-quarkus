
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.enums.DataType;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.camunda.spin.json.SpinJsonNode;

import static com.fasterxml.jackson.core.JsonPointer.compile;
import static java.util.stream.Collectors.toConcurrentMap;
import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 * This class represents a JSON utility.
 * It is annotated with @Slf4j.
 * This annotation provides a logger.
 * The utility includes methods to generate JSON and set a JSON element.
 */
@Slf4j
public final class JsonUtils {

  private static final Map<DataType, UnaryOperator<Object>> typeConversionMap = new EnumMap<>(DataType.class);

  private JsonUtils() {
  }

  static {
    typeConversionMap.put(DataType.INTEGER, data -> new IntNode((Integer) data));
    typeConversionMap.put(DataType.DOUBLE, data -> new DoubleNode((Double) data));
    typeConversionMap.put(DataType.SHORT, data -> new LongNode((Short) data));
    typeConversionMap.put(DataType.LONG, data -> new LongNode((Long) data));
    typeConversionMap.put(DataType.STRING, (Object data) -> {
      var string = (String) data;
      if (isJson(string)) {
        log.info("String {} can be converted to json", string);
        JsonNode json = ConversionUtils.convertToJson(string);
        return new POJONode(json);
      } else {
        return new TextNode(string);
      }
    });
    typeConversionMap.put(DataType.SPIN_JSON_NODE, (Object data) -> {
      log.info("{} is of type SpinJsonNode", data);
      try {
        return new POJONode(Config.OBJECT_MAPPER.readTree(data.toString()));
      } catch (JsonProcessingException e) {
        throw new ObjectMappingException("Error while mapping SpinJsonNode to JsonNode", e);
      }
    });
    typeConversionMap.put(DataType.DEFAULT, POJONode::new);
  }

  /**
   * Generating json with all the json paths and their corresponding values
   *
   * @param map input map with key as json paths and their data as values
   * @return generated json
   */
  public static JsonNode generateJson(Map<String, Object> map) {

    map = reformatJsonPaths(map);

    var json = Config.OBJECT_MAPPER.createObjectNode();
    if (MapUtils.isNotEmpty(map)) {
      map.forEach((jsonPath, data) -> setJsonElement(json, compile(jsonPath), getValue(data)));
    }

    if (json.has("")) {
      return json.get("");
    }

    return json;
  }

  /**
   * Sets a data in the specified json path of a json Creates the json nodes in
   * the json path if the path doesn't exist
   *
   * @param node    the json in which the data has to be set in the specified path
   * @param pointer the json path where the value has to be set
   * @param value   the data node to be set in the json path
   */
  public static void setJsonElement(ObjectNode node, JsonPointer pointer, JsonNode value) {

    JsonPointer parentPointer = pointer.head();
    JsonNode parentNode = node.at(parentPointer);
    var fieldName = pointer.last().toString().substring(1);

    if (parentNode.isMissingNode() || parentNode.isNull()) {
      parentNode = isArrayNode(fieldName) ? Config.OBJECT_MAPPER.createArrayNode() : Config.OBJECT_MAPPER.createObjectNode();
      setJsonElement(node, parentPointer, parentNode);
    }

    if (parentNode.isArray()) {
      var arrayNode = (ArrayNode) parentNode;
      int index = "*".equals(fieldName) ? arrayNode.size() : Integer.valueOf(fieldName);
      // expand array in case index is greater than array size (like JavaScript does)
      IntStream.rangeClosed(arrayNode.size(), index).forEach(i -> arrayNode.addNull());
      arrayNode.set(index, value);
    } else if (parentNode.isObject()) {
      ((ObjectNode) parentNode).set(fieldName, value);
    } else {
      throw new NonRetryableException(String.format("`%s` can't be set for parent node `%s` " +
        "because parent is not a container but %s", fieldName, parentPointer, parentNode.getNodeType().name()));
    }
  }

  private static boolean isArrayNode(String field) {

    if (isNumeric(field)) {
      return true;
    } else {
      return "*".equals(field);
    }
  }

  /**
   * Creates a data node based on the type of the value
   *
   * @param data the data to be converted to the json node
   * @param <T>
   * @return the newly created json data node
   */
  @SneakyThrows
  public static <T> ValueNode getValue(T data) {
    if (ObjectUtils.isEmpty(data)) {
      return NullNode.getInstance();
    }

    var dataType = getDataTypeFromClass(data.getClass());
    UnaryOperator<Object> converter = typeConversionMap.getOrDefault(dataType, typeConversionMap.get(DataType.DEFAULT));
    return (ValueNode) converter.apply(data);
  }

  private static DataType getDataTypeFromClass(Class<?> clazz) {
    String className = clazz.getSimpleName().toUpperCase(Locale.ROOT);
    try {
      return DataType.valueOf(className);
    } catch (IllegalArgumentException e) {
      if (SpinJsonNode.class.isAssignableFrom(clazz)) {
        return DataType.SPIN_JSON_NODE;
      }
      log.error("Error while getting data type from class: ", e);
      return DataType.DEFAULT;
    }
  }


  /**
   * Reformat json paths.
   *
   * @param map the map
   * @return the map
   */
  public static Map<String, Object> reformatJsonPaths(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(entry -> new SimpleEntry<>(reformatJsonPath(entry.getKey()), entry.getValue()))
      .filter(entry -> ObjectUtils.isNotEmpty(entry.getValue()))
      .collect(toConcurrentMap(Entry::getKey, Entry::getValue));
  }


  /**
   * Reformat json path string.
   *
   * @param jsonPath the json path
   * @return the string
   */
  private static String reformatJsonPath(String jsonPath) {
    if ("$".equals(jsonPath) || "_$".equals(jsonPath)) {
      jsonPath = BobConstants.PATH_DELIMITER;
    }

    jsonPath = jsonPath.replace(".[*]", "[*]")
      .replace("[*]", BobConstants.PATH_DELIMITER + "[*]")
      .replace(".", BobConstants.PATH_DELIMITER)
      .replace("[", "")
      .replace("]", "");

    if (!(jsonPath.startsWith(BobConstants.PATH_DELIMITER) || jsonPath.startsWith("$"))) {
      jsonPath = BobConstants.PATH_DELIMITER + jsonPath;
    }

    return jsonPath;
  }

  /**
   * Checks if the given data is a valid json.
   *
   * @param data the data
   * @return the boolean
   */
  public static boolean isJson(Object data) {

    if (ObjectUtils.isNotEmpty(data)) {
      try {
        Config.OBJECT_MAPPER.readTree(data.toString());
        return true;
      } catch (IOException e) {
        log.error("Error while mapping object to json: ", e);
        return false;
      }
    }
    return false;
  }
}
