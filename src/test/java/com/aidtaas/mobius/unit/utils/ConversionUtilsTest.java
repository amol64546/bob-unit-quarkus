//package com.aidtaas.mobius.consumer.utils;
//
//import com.aidtaas.mobius.consumer.dto.GlobalVariable;
//import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import io.quarkus.test.junit.QuarkusTest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//@QuarkusTest
//class ConversionUtilsTest {
//
//  private ConversionUtils conversionUtils;
//
//  @BeforeEach
//  void setUp() {
//  }
//
//  @Test
//  void testConvertObjectShouldReturnConvertedObject() {
//    String object = "test";
//    String result = ConversionUtils.convertObject(object, String.class);
//    assertEquals(object, result, "Expected the converted object to be equal to the original object");
//  }
//
//  @Test
//  void testConvertObjectShouldThrowExceptionForIncompatibleTypes() {
//    String object = "test";
//    assertThrows(IllegalArgumentException.class, () -> ConversionUtils.convertObject(object, Integer.class), "Expected IllegalArgumentException when trying to convert incompatible types");
//  }
//
//  @Test
//  void testTypeCastShouldReturnCastedObject() {
//    String value = "123";
//    Integer result = ConversionUtils.typeCast(value, Integer.class);
//    assertEquals(Integer.valueOf(value), result, "Expected the casted object to be equal to the original object");
//  }
//
//  @Test
//  void testTypeCastShouldThrowExceptionForUndefinedType() {
//    String value = "test";
//    assertThrows(UnsupportedOperationException.class, () -> ConversionUtils.typeCast(value, null), "Expected UnsupportedOperationException when type is undefined");
//  }
//
//  @Test
//  void testConvertStringShouldReturnConvertedObject() {
//    String jsonString = "{\"name\":\"test\"}";
//    GlobalVariable result = ConversionUtils.convertString(jsonString, GlobalVariable.class);
//    assertEquals("test", result.getName(), "Expected the converted object to have the same values as the original JSON string");
//  }
//
//  @Test
//  void testConvertStringShouldThrowExceptionForInvalidJson() {
//    String jsonString = "invalid json";
//    assertThrows(ObjectMappingException.class, () -> ConversionUtils.convertString(jsonString, GlobalVariable.class), "Expected ObjectMappingException when trying to convert invalid JSON string");
//  }
//
//  @Test
//  void testConvertToJsonShouldReturnJsonNode() {
//    String data = "{\"name\":\"test\"}";
//    JsonNode result = ConversionUtils.convertToJson(data);
//    assertEquals("test", result.get("name").asText(), "Expected the converted JsonNode to have the same values as the original data");
//  }
//
//  @Test
//  void testConvertToJsonShouldReturnNullForNullData() {
//    JsonNode result = ConversionUtils.convertToJson(null);
//    assertNull(result, "Expected the converted JsonNode to be null when the data is null");
//  }
//}