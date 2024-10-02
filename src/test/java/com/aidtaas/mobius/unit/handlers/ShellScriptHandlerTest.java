//package com.aidtaas.mobius.consumer.handlers;
//
//import com.aidtaas.mobius.consumer.handlers.ShellScriptHandler;
//import com.jcraft.jsch.ChannelExec;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.Session;
//import java.io.BufferedInputStream;
//import java.io.BufferedReader;
//import java.io.StringReader;
//import org.apache.commons.io.input.ReaderInputStream;
//import org.camunda.bpm.client.task.ExternalTask;
//import org.camunda.bpm.client.task.ExternalTaskService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class ShellScriptHandlerTest {
//
//    @InjectMocks
//    ShellScriptHandler shellScriptHandler;
//
//    @Mock
//    ExternalTask externalTask;
//
//    @Mock
//    ExternalTaskService externalTaskService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void testExecuteExternalTaskSuccessfully() {
//        shellScriptHandler.execute(externalTask, externalTaskService);
//        verify(externalTask, times(1)).getAllVariables();
//        verify(externalTaskService, times(1)).complete(externalTask);
//    }
//
//    @Test
//    void testHandleFailureWhenExceptionOccurs() {
//        doThrow(new RuntimeException("Test exception")).when(externalTask).getAllVariables();
//        shellScriptHandler.execute(externalTask, externalTaskService);
//        verify(externalTaskService, times(1)).handleFailure(any(ExternalTask.class), any(), any(), anyInt(), anyInt());
//    }
//
//    @Test
//    void testThrowExceptionWhenCommandExecutionFails() {
//        assertThrows(Exception.class, () -> shellScriptHandler.executeCommand("user", "privateKeyPath", "host", 22, "invalidScript", "password"), "Expected exception to be thrown");
//    }
//}