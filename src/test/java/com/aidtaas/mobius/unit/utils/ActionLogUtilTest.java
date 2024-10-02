//package com.aidtaas.mobius.consumer.utils;
//
//import com.aidtaas.mobius.consumer.aclmodels.ActionSource;
//import com.aidtaas.mobius.consumer.aclmodels.ActionType;
//import com.aidtaas.mobius.consumer.aclmodels.NodeType;
//import com.aidtaas.mobius.consumer.aclmodels.RequesterType;
//import com.aidtaas.mobius.consumer.config.DynamicRestClient;
//import io.quarkus.test.junit.QuarkusTest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@QuarkusTest
//class ActionLogUtilTest {
//
//  @Mock
//  private DynamicRestClient dynamicRestClient;
//
//  @InjectMocks
//  private ActionLogUtil actionLogUtil;
//
//  @BeforeEach
//  void setUp() {
//    MockitoAnnotations.openMocks(this);
//  }
//
//  @Test
//  void testActionLogCallsMakeApiCall() {
//    actionLogUtil.actionLog(ActionSource.HUMAN_API, ActionType.CREATE, RequesterType.TENANT, "requesterId", NodeType.TENANT, "nodeId");
//    verify(dynamicRestClient, times(1)).makeApiCall(any(), any(), anyString(), any());
//  }
//
//}