//package com.aidtaas.mobius.consumer.utils;
//
//import com.aidtaas.mobius.consumer.config.Config;
//import com.fasterxml.jackson.core.JsonPointer;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.IntNode;
//import com.fasterxml.jackson.databind.node.TextNode;
//import io.quarkus.test.junit.QuarkusTest;
//import java.util.HashMap;
//import java.util.Map;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//@QuarkusTest
//class JsonUtilsTest {
//
//  private Map<String, Object> map;
//
//  @BeforeEach
//  void setUp() {
//    map = new HashMap<>();
//  }
//
//  @Test
//  void testGenerateJsonWithEmptyMapReturnsEmptyJson() {
//    JsonNode result = JsonUtils.generateJson(map);
//    assertTrue(result.isEmpty(), "Expected the result to be empty");
//  }
//
//  @Test
//  void testGenerateJsonWithNonEmptyMapReturnsNonEmptyJson() {
//    map.put("/key", "value");
//    JsonNode result = JsonUtils.generateJson(map);
//    assertFalse(result.isEmpty(), "Expected the result to be non-empty");
//    assertEquals("value", result.get("key").asText(), "Expected the value of 'key' to be 'value'");
//  }
//
//  @Test
//  void testSetJsonElementWithValidInputSetsElementInJson() {
//    var node = Config.mapper.createObjectNode();
//    var pointer = JsonPointer.compile("/key");
//    var value = new TextNode("value");
//    JsonUtils.setJsonElement(node, pointer, value);
//    assertEquals("value", node.get("key").asText(), "Expected the value of 'key' to be 'value'");
//  }
//
//  @Test
//  void testGetValueWithNullReturnsNullNode() {
//    var result = JsonUtils.getValue(null);
//    assertTrue(result.isNull(), "Expected the result to be null");
//  }
//
//  @Test
//  void testGetValueWithIntegerReturnsIntNode() {
//    var result = JsonUtils.getValue(1);
//    assertTrue(result instanceof IntNode, "Expected the result to be an instance of IntNode");
//    assertEquals(1, result.asInt(), "Expected the result to be 1");
//  }
//
//  @Test
//  void testReformatJsonPathsWithValidInputReturnsReformattedPaths() {
//    map.put("$.key", "value");
//    var result = JsonUtils.reformatJsonPaths(map);
//    assertTrue(result.containsKey("$/key"), "Expected the result to contain '/key'");
//  }
//
//  @Test
//  void testIsJsonWithValidJsonReturnsTrue() {
//    assertTrue(JsonUtils.isJson("{\"key\":\"value\"}"), "Expected the result to be true");
//  }
//
//  @Test
//  void testIsJsonWithInvalidJsonReturnsFalse() {
//    assertFalse(JsonUtils.isJson("not a json"), "Expected the result to be false");
//  }
//}