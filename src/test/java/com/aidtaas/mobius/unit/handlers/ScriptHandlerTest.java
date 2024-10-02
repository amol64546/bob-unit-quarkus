//package com.aidtaas.mobius.consumer.handlers;
//
//import io.quarkus.test.junit.QuarkusTest;
//import org.camunda.bpm.client.task.ExternalTask;
//import org.camunda.bpm.client.task.ExternalTaskService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@QuarkusTest
//class ScriptHandlerTest {
//
//  @InjectMocks
//  ScriptHandler scriptHandler;
//
//  @Mock
//  ExternalTask externalTask;
//
//  @Mock
//  ExternalTaskService externalTaskService;
//
//  @BeforeEach
//  void setUp() {
//    MockitoAnnotations.openMocks(this);
//  }
//
//  @Test
//  void testShouldExecuteExternalTask() {
//    scriptHandler.execute(externalTask, externalTaskService);
//    verify(externalTask, times(1)).getAllVariables();
//  }
//}