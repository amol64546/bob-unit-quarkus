
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiMeteringDTO;
import com.aidtaas.mobius.unit.dto.ApiOperation;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.dto.JobStatusDTO;
import com.aidtaas.mobius.unit.enums.Environment;
import com.aidtaas.mobius.unit.grpc.ApiInformation;
import com.aidtaas.mobius.unit.grpc.ApiMetering;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.camunda.bpm.client.task.ExternalTask;

import static com.aidtaas.mobius.unit.constants.BobConstants.CONTENT_TYPE;
import static com.aidtaas.mobius.unit.constants.BobConstants.HTTP_STATUS_CODE_200;
import static com.aidtaas.mobius.unit.constants.BobConstants.HTTP_STATUS_CODE_300;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

/**
 * This class represents metering utilities.
 * It includes methods to create metering statistics.
 */
@Slf4j
public final class MeteringUtils {


  private MeteringUtils() {
  }

  /**
   * Creates a new metering job statistic.
   *
   * @param externalTask the external task
   * @return the new metering job statistic
   */
  public static JobStatusDTO newMeteringJobStat(ExternalTask externalTask) {

    return JobStatusDTO.builder()
      .id(Instant.now().toEpochMilli() + randomUUID().toString())
      .tenantId(externalTask.getVariable(BobConstants.TENANT_ID_GLOBAL))
      .workflowProcessInstanceId(externalTask.getProcessInstanceId())
      .build();
  }


  /**
   * Creates a new metering statistic.
   *
   * @param apiOperation the API operation
   * @param externalTask the external task
   * @return the new metering statistic
   */
  public static ApiMeteringDTO newMeteringStat(ApiOperation apiOperation, ExternalTask externalTask) {

    var apiMeteringDTO = newMeteringStatsWithExecution(externalTask);

    ofNullable(apiOperation.getAppId()).ifPresent(apiMeteringDTO::setAppId);
    ofNullable(apiOperation.getProductId()).ifPresent(apiMeteringDTO::setProductId);
    ofNullable(apiOperation.getAllianceId()).ifPresent(apiMeteringDTO::setAllianceId);
    ofNullable(apiOperation.getExecutorTenantId()).ifPresent(apiMeteringDTO::setTenantId);
    ofNullable(apiOperation.getProductOwnerId()).ifPresent(apiMeteringDTO::setProductOwner);
    ofNullable(apiOperation.getEnvironment()).map(Object::toString).ifPresent(apiMeteringDTO::setMode);

    setFileSize(externalTask, apiMeteringDTO);

    return apiMeteringDTO;
  }

  private static void setFileSize(ExternalTask externalTask, ApiMeteringDTO apiMeteringDTO) {

    var fileSizeInBytes = new AtomicLong(0L);

    externalTask.getAllVariables().forEach((String key, Object value) -> {
      if (Objects.equals(key, "fileInput")) {
        log.info("File found in variable: {}", key);
        ByteArrayInputStream file = (ByteArrayInputStream) value;
        fileSizeInBytes.set(file.readAllBytes().length);
      }
    });

    long size = fileSizeInBytes.get();
    log.info("File size: {}", fileSizeInBytes);
    apiMeteringDTO.setFileSizeInBytes(size);

  }


  /**
   * Creates a new metering statistic.
   *
   * @param externalTask the external task
   * @return the new metering statistic
   */
  public static ApiMeteringDTO newMeteringStatsWithExecution(ExternalTask externalTask) {
    ApiMeteringDTO apiMeteringDTO = new ApiMeteringDTO();
    apiMeteringDTO.setId(Instant.now().toEpochMilli() + randomUUID().toString());
    apiMeteringDTO.setWorkflowId(CommonUtils.getWorkflowId(externalTask));
    apiMeteringDTO.setWorkflowProcessInstanceId(externalTask.getProcessInstanceId());
    apiMeteringDTO.setExecutedAt(Instant.now().toEpochMilli());
    apiMeteringDTO.setCreatedAt(Instant.now().toEpochMilli());
    apiMeteringDTO.setActivityInstanceId(externalTask.getActivityInstanceId());
    apiMeteringDTO.setActivityId(externalTask.getActivityId());
    return apiMeteringDTO;
  }

  /**
   * Creates a new metering statistic.
   *
   * @param apiOperation the API operation
   * @param externalTask the external task
   * @param body         the body
   * @param resolvedUrl  the resolved URL
   * @param httpMethod   the HTTP method
   * @param apiResponse  the API response
   * @return the new metering statistic
   */
  public static ApiMetering newMeteringStatGrpc(ApiOperation apiOperation, ExternalTask externalTask, Object body,
                                                String resolvedUrl, String httpMethod, ApiResponseBody apiResponse, Map<String, String> headers) {

    var apiMeteringBuilder = ApiMetering.newBuilder()
      .setExecutedAt(Instant.now().getEpochSecond())
      .setAppId(getValueOrDefault(apiOperation.getAppId()))
      .setProductId(getValueOrDefault(apiOperation.getInput().getComponentId()))
      .setAllianceId(getValueOrDefault(apiOperation.getAllianceId()))
      .setMode((ObjectUtils.isNotEmpty(apiOperation.getEnvironment()) ?
        apiOperation.getEnvironment() : Environment.TEST).toString())
      .setId(Instant.now().toEpochMilli() + randomUUID().toString())
      .setWorkflowId(getValueOrDefault(CommonUtils.getWorkflowId(externalTask)))
      .setWorkflowProcessInstanceId(getValueOrDefault(externalTask.getProcessInstanceId()))
      .setActivityInstanceId(getValueOrDefault(externalTask.getActivityId()))
      .setActivityId(getValueOrDefault(externalTask.getActivityId()))
      .setApiInformation(getApiInformation(resolvedUrl, httpMethod, body, apiResponse))
      .setResponseStatus(ObjectUtils.isNotEmpty(apiResponse) ? apiResponse.getStatusCodeValue() : 0)
      .setTenantType(getTenantType(apiOperation))
      .setTenantId(getValueOrDefault(apiOperation.getExecutorTenantId()))
      .setSource(apiOperation.getInput().getComponentId())
      .setDataSize(setDataSize(externalTask, apiResponse.getHeaders()))
      .setRequestType(headers.get(CONTENT_TYPE));

    return apiMeteringBuilder.build();
  }
  
  private static long setDataSize(ExternalTask externalTask, MultivaluedMap<String, Object> headers) {

    long downloadFileSize = 0;

    if(headers.containsKey("Content-Disposition")) {
      String contentDisposition = headers.get("Content-Disposition").toString();
      if(contentDisposition.contains("attachment")) {
        downloadFileSize = Long.parseLong(headers.get("Content-Length").toString());
      }
    } else if (headers.get(CONTENT_TYPE).toString().contains("application/octet-stream")) {
      downloadFileSize = Long.parseLong(headers.get("Content-Length").toString());
    }

    var fileSizeInBytes = new AtomicLong(0L);

    externalTask.getAllVariables().forEach((String key, Object value) -> {
      if (Objects.equals(key, "fileInput")) {
        log.info("File found in variable: {}", key);
        ByteArrayInputStream file = (ByteArrayInputStream) value;
        fileSizeInBytes.set(file.readAllBytes().length);
      }
    });

    long size = fileSizeInBytes.get()+downloadFileSize;
    log.info("File size: {}", fileSizeInBytes);
    return size;
  }

  private static String getValueOrDefault(String value) {
    return ObjectUtils.isNotEmpty(value) ? value : "";
  }

  /**
   * Gets API information.
   *
   * @param resolvedUrl the resolved URL
   * @param httpMethod  the HTTP method
   * @param body        the body
   * @param apiResponse the API response
   * @return the API information
   */
  private static ApiInformation getApiInformation(String resolvedUrl, String httpMethod,
                                                  Object body, ApiResponseBody apiResponse) {
    return ApiInformation.newBuilder()
      .setApiProduct(getValueOrDefault(resolvedUrl))
      .setApiMethod(getValueOrDefault(httpMethod))
      .setApiBody(getValueOrDefault(ObjectUtils.isNotEmpty(body) ? body.toString() : null))
      .setApiResponse(getValueOrDefault(ObjectUtils.isNotEmpty(apiResponse) ? apiResponse.toString() : null))
      .setApiResponseBodySize(getApiResponseBodySize(apiResponse))
      .setApiRequestBodySize(body != null ? body.toString().getBytes().length : 0)
      .build();
  }

  /**
   * Gets the API response body size.
   *
   * @param apiResponse the API response
   * @return the API response body size
   */
  private static int getApiResponseBodySize(ApiResponseBody apiResponse) {
    if (isResponseValid(apiResponse) && ObjectUtils.isNotEmpty(apiResponse.getBody())) {
      return apiResponse.getBody().getBytes().length;
    }
    return 0;
  }

  /**
   * Checks if the API response is valid.
   *
   * @param apiResponse the API response
   * @return true if the API response is valid, false otherwise
   */
  private static boolean isResponseValid(ApiResponseBody apiResponse) {
    return ObjectUtils.isNotEmpty(apiResponse) &&
      apiResponse.getStatusCodeValue() >= HTTP_STATUS_CODE_200 &&
      apiResponse.getStatusCodeValue() < HTTP_STATUS_CODE_300;
  }

  /**
   * Gets the tenant type.
   *
   * @param apiOperation the API operation
   * @return the tenant type
   */
  private static String getTenantType(ApiOperation apiOperation) {
    return Environment.TEST.getValue().equalsIgnoreCase
      (apiOperation.getEnvironment().getValue()) ? BobConstants.TP_TENANT : "";
  }
}
