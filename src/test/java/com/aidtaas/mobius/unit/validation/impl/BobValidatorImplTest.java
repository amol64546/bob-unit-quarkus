//package com.aidtaas.mobius.consumer.validation.impl;
//
//import com.aidtaas.mobius.consumer.constants.BobConsumerConstants;
//import com.aidtaas.mobius.consumer.dto.ApiOperation;
//import com.aidtaas.mobius.consumer.exception.NonRetryableException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.quarkus.test.junit.QuarkusTest;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.stream.Stream;
//import org.camunda.bpm.client.task.ExternalTask;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import static java.lang.String.format;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@QuarkusTest
//class BobValidatorImplTest {
//
//  @InjectMocks
//  private BobValidatorImpl bobValidator;
//
//  @Mock
//  private ApiOperation apiOperation;
//
//  @Mock
//  private ExternalTask externalTask;
//
//  @BeforeEach
//  void setUp() {
//    MockitoAnnotations.openMocks(this);
//  }
//
//  @ParameterizedTest
//  @MethodSource("provideJsonForTesting")
//  void testThrowExceptionWhenFieldIsMissing(String jsonInput, String errorMessage) {
//    when(apiOperation.getExternalTask()).thenReturn(externalTask);
//    when(externalTask.getVariable("inputKey")).thenReturn(jsonInput);
//
//    assertThrows(NonRetryableException.class, () -> bobValidator.validateApiOperation(apiOperation), errorMessage);
//  }
//
//  private static Stream<Arguments> provideJsonForTesting() {
//    return Stream.of(
//      Arguments.of("{\"COMPONENT_ID\": null}", "Component ID is missing"),
//      Arguments.of("{\"PRODUCT_MASTER_CONFIG_ID\": null}", "Product Master Config ID is missing"),
//      Arguments.of("{\"INTERFACE_PATH\": null}", "Interface path is missing"),
//      Arguments.of("{\"ITEMS\": null}", "Component ID, Product Master Config ID, and Interface Path are missing")
//    );
//  }
//
//  //  @Test
//  void testNotThrowExceptionWhenAllRequiredFieldsArePresent() throws Exception {
//    String jsonInput = new String(Files.readAllBytes(Paths.get("src/test/resources/apiOperator.json")));
//    String inputKey = new String(Files.readAllBytes(Paths.get("src/test/resources/inputKey.json")));
//    String outputKey = new String(Files.readAllBytes(Paths.get("src/test/resources/outputKey.json")));
//
//
//    ExternalTask externalTask1 = mock(ExternalTask.class);
//
//
//    ObjectMapper objectMapper = new ObjectMapper();
//    ApiOperation operation = objectMapper.readValue(jsonInput, ApiOperation.class);
//    apiOperation.setExternalTask(externalTask1);
//    String input = format(BobConsumerConstants.INPUT_KEY_FORMAT, "1234");
//    String output = format(BobConsumerConstants.OUTPUT_KEY_FORMAT, "1234");
//    when(apiOperation.getExternalTask()).thenReturn(externalTask1);
//    when(externalTask1.getProcessInstanceId()).thenReturn("1234");
//    when(externalTask.getTopicName()).thenReturn("apiOperationHandler");
//    when(externalTask.getActivityId()).thenReturn("1234");
//    when(externalTask.getVariable(input)).thenReturn(inputKey);
//    when(externalTask.getVariable(output)).thenReturn(outputKey);
//
//    bobValidator.validateApiOperation(operation);
//  }
//}