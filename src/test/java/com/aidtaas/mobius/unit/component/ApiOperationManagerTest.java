//package com.aidtaas.mobius.consumer.component;
//
//import com.aidtaas.mobius.consumer.config.DynamicRestClient;
//import com.aidtaas.mobius.consumer.config.URLResolver;
//import com.aidtaas.mobius.consumer.enums.Environment;
//import com.aidtaas.mobius.consumer.dto.RestApiOperation;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.redisson.cache.Cache;
//
//import static org.mockito.Mockito.*;
//
//class ApiOperationManagerTest {
//
//    @InjectMocks
//    ApiOperationManager apiOperationManager;
//
//    @Mock
//    DynamicRestClient restTemplate;
//
//    @Mock
//    URLResolver urlResolver;
//
//    @Mock
//    Cache<String, String> cache;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void testShouldRetrieveRestApiInfoForTestEnvironment() {
//        RestApiOperation restApiOperation = new RestApiOperation();
//        restApiOperation.setEnvironment(Environment.TEST);
//        apiOperationManager.retrieveRestApiInfo(restApiOperation);
//        verify(restTemplate, times(1)).makeApiCall(anyString(), any(), anyString(), anyMap());
//    }
//
//    @Test
//    void testShouldRetrieveRestApiInfoForProdEnvironment() {
//        RestApiOperation restApiOperation = new RestApiOperation();
//        restApiOperation.setEnvironment(Environment.PROD);
//        apiOperationManager.retrieveRestApiInfo(restApiOperation);
//        verify(restTemplate, times(2)).makeApiCall(anyString(), any(), anyString(), anyMap());
//    }
//
//    @Test
//    void testShouldRetrieveAllianceConfig() {
//        apiOperationManager.retrieveAllianceConfig("creatorId", "appId", "auth");
//        verify(restTemplate, times(1)).makeApiCall(anyString(), any(), anyString(), anyMap());
//    }
//
//    @Test
//    void testShouldRetrieveProductConfigForSpecificApis() {
//        apiOperationManager.retrieveProductConfigForSpecificApis(PRODUCT_ID, "productMasterConfigId", "apiPath", "auth");
//        verify(restTemplate, times(1)).makeApiCall(anyString(), any(), anyString(), anyMap());
//    }
//
//    @Test
//    void testShouldGetCacheValue() {
//        apiOperationManager.getCacheValue("apiPath", PRODUCT_ID, "productMasterConfigId");
//        verify(cache, times(1)).get(anyString());
//    }
//
//    @Test
//    void testShouldAddCacheValue() {
//        apiOperationManager.addCacheValue("apiPath", PRODUCT_ID, "productMasterConfigId", "productJsonString");
//        verify(cache, times(1)).put(anyString(), anyString());
//    }
//}