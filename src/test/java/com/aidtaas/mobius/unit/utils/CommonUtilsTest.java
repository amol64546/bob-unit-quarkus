//package com.aidtaas.mobius.consumer.utils;
//
//import com.aidtaas.mobius.consumer.dto.InMemoryFile;
//import io.quarkus.test.junit.QuarkusTest;
//import java.util.Collection;
//import org.camunda.bpm.engine.variable.Variables;
//import org.camunda.bpm.engine.variable.value.FileValue;
//import org.camunda.bpm.engine.variable.value.TypedValue;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.Arrays;
//import java.util.List;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@QuarkusTest
//class CommonUtilsTest {
//
//    private static final Logger log = LoggerFactory.getLogger(CommonUtilsTest.class);
//    private FileValue fileValue;
//    private TypedValue typedValue;
//
//    @BeforeEach
//    void setUp() {
//        fileValue = Variables.fileValue("test.txt").file("test content".getBytes()).create();
//        typedValue = Variables.stringValue("test");
//    }
//
//    @Test
//    void testSanitizeProcessValueWithFileValueReturnsInMemoryFile() {
//        Object result = CommonUtils.sanitizeProcessValue(fileValue);
//        assertTrue(result instanceof InMemoryFile, "Expected result to be an instance of InMemoryFile");
//    }
//
//    @Test
//    void testSanitizeProcessValueWithTypedValueReturnsValue() {
//        Object result = CommonUtils.sanitizeProcessValue(typedValue);
//        assertEquals("test", result, "Expected result to be 'test'");
//    }
//
//    @Test
//    void testSanitizeProcessValueWithNullReturnsNull() {
//        Object result = CommonUtils.sanitizeProcessValue(null);
//        assertNull(result, "Expected result to be null");
//    }
//
//}