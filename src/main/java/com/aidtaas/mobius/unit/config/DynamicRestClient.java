
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import com.aidtaas.mobius.unit.constants.BobConstants;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.dto.InMemoryFile;
import com.aidtaas.mobius.unit.enums.HttpMethod;
import com.aidtaas.mobius.unit.exception.NonRetryableException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.HeaderGroup;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.http.Consts;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import static com.aidtaas.mobius.unit.constants.BobConstants.APPLICATION_X_WWW_FORM_URLENCODED;
import static com.aidtaas.mobius.unit.constants.BobConstants.CONTENT_TYPE;
import static com.aidtaas.mobius.unit.constants.BobConstants.HTTP_STATUS_CODE_200;
import static com.aidtaas.mobius.unit.constants.BobConstants.HTTP_STATUS_CODE_300;

@Slf4j
@ApplicationScoped
public class DynamicRestClient {

  private static final EnumMap<HttpMethod, TriFunction<GenericRestClient, Map<String, String>, Object, Response>> methodMap =
    new EnumMap<>(HttpMethod.class);

  private final CloseableHttpClient httpClient;
  private final ConfigProperties config;

  public DynamicRestClient(@Named("BobHttpClient") CloseableHttpClient httpClient, ConfigProperties config) {
    this.httpClient = httpClient;
    this.config = config;
  }

  @PostConstruct
  public void init() {
    methodMap.put(HttpMethod.POST, (GenericRestClient client, Map<String, String> headers, Object requestBody) ->
      client.invokePostEndpoint(requestBody));
    methodMap.put(HttpMethod.GET, (GenericRestClient client, Map<String, String> headers, Object requestBody) ->
      client.invokeGetEndpoint());
    methodMap.put(HttpMethod.PUT, (GenericRestClient client, Map<String, String> headers, Object requestBody) ->
      client.invokePutEndpoint(requestBody));
    methodMap.put(HttpMethod.DELETE, (GenericRestClient client, Map<String, String> headers, Object requestBody) ->
      client.invokeDeleteEndpoint());
  }

  /**
   * Get or create a GenericRestClient for the given base URL.
   *
   * @param baseUrl the base URL for the client
   * @return the RestClient instance
   */
  private GenericRestClient getClientForBaseUrl(String baseUrl, Map<String, String> headers) {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(baseUrl))
      .register(new DynamicHeaderFilter(headers))
      .followRedirects(true)
      .connectTimeout(config.restClientConnectTimeout(), TimeUnit.SECONDS)
      .readTimeout(config.restClientReadTimeout(), TimeUnit.SECONDS)
      .build(GenericRestClient.class);
  }

  /**
   * Make API call.
   *
   * @param endpointUrl the endpoint url
   * @param requestBody the request body
   * @param httpMethod  the http method
   * @param headers     the headers
   * @return the api response body
   */
  public ApiResponseBody makeApiCall(String endpointUrl, Object requestBody,
                                     String httpMethod, Map<String, String> headers) {

    if (MapUtils.isEmpty(headers)) {
      headers = new HashMap<>();
    }
    var method = HttpMethod.valueOf(httpMethod);
    TriFunction<GenericRestClient, Map<String, String>, Object, Response> action = methodMap.get(method);

    if (ObjectUtils.isEmpty(action)) {
      throw new NonRetryableException(String.format("Invalid HTTP method: %s", httpMethod));
    }

    GenericRestClient client = getClientForBaseUrl(endpointUrl, headers);

    Response response = action.apply(client, headers, requestBody);

    int statusCode = response.getStatus();
    MultivaluedMap<String, Object> responseHeaders = response.getHeaders();

    String res = response.readEntity(String.class);

    var apiResponseBody = new ApiResponseBody();
    apiResponseBody.setBody(res);
    apiResponseBody.setStatusCodeValue(statusCode);
    apiResponseBody.setHeaders(responseHeaders);

    return apiResponseBody;
  }

  public ApiResponseBody makeMultipartApiCall(String endpointUrl, Map<String, Object> requestBody,
                                              String httpMethod, Map<String, String> reqHeaders) {

    HttpUriRequestBase httpRequest;
    if (HttpMethod.POST.getValue().equalsIgnoreCase(httpMethod)) {
      httpRequest = new HttpPost(endpointUrl);
    } else if (HttpMethod.PUT.getValue().equalsIgnoreCase(httpMethod)) {
      httpRequest = new HttpPut(endpointUrl);
    } else {
      throw new NonRetryableException("Unsupported HTTP method for form data: " + httpMethod);
    }

    if (reqHeaders.get(CONTENT_TYPE).equalsIgnoreCase(APPLICATION_X_WWW_FORM_URLENCODED)) {
      List<NameValuePair> params = new ArrayList<>();
      requestBody.forEach((key, value) -> params.add(new BasicNameValuePair(key, value.toString())));
      HttpEntity urlEncodedEntity = new UrlEncodedFormEntity(params, Consts.UTF_8);
      httpRequest.setEntity(urlEncodedEntity);
    } else {
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      requestBody.forEach((String key, Object value) -> {
        if (value instanceof byte[] byteValue) {
          builder.addBinaryBody(key, byteValue, ContentType.APPLICATION_OCTET_STREAM, key);
        } else if (value instanceof InMemoryFile inMemoryFile) {
          builder.addBinaryBody(key, inMemoryFile.getFileData(), ContentType.APPLICATION_OCTET_STREAM, inMemoryFile.getFilename());
        } else {
          builder.addTextBody(key, value.toString(), ContentType.TEXT_PLAIN);
        }
      });
      HttpEntity multipart = builder.build();
      httpRequest.setEntity(multipart);
    }

    reqHeaders.forEach(httpRequest::setHeader);
    httpRequest.removeHeaders(CONTENT_TYPE);

    try {
      HttpContext context = new BasicHttpContext();
      ApiResponseBody deploymentResponse = httpClient.execute(httpRequest, context, DynamicRestClient::getApiResponse);
      log.info("end of deployToCamunda method of CamundaHelperUtils class with deploymentResponse: {}",
        deploymentResponse);
      return deploymentResponse;
    } catch (IOException e) {
      throw new NonRetryableException("Error while making API call", e);
    }
  }

  private static ApiResponseBody getApiResponse(ClassicHttpResponse response) throws IOException, ParseException {
    HttpEntity responseEntity = response.getEntity();
    String responseBody = BobConstants.NO_CONTENT_FROM_THE_RESPONSE;
    if (ObjectUtils.isNotEmpty(responseEntity)) {
      responseBody = EntityUtils.toString(responseEntity);
    }
    ApiResponseBody apiResponseBody = new ApiResponseBody();
    apiResponseBody.setStatusCodeValue(response.getCode());
    if (response.getCode() >= HTTP_STATUS_CODE_200 && response.getCode() < HTTP_STATUS_CODE_300) {

      apiResponseBody.setBody(responseBody);

      HeaderGroup headers = new HeaderGroup();
      response.headerIterator().forEachRemaining(headers::addHeader);
      MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
      Arrays.stream(headers.getHeaders()).forEach(header -> responseHeaders.add(header.getName(), header.getValue()));
      apiResponseBody.setHeaders(responseHeaders);

      return apiResponseBody;
    } else {
      log.warn("Error executing the API with error code: {}", response.getCode());
      Map<String, String> errorMessage = new HashMap<>();
      if(StringUtils.isNotEmpty(responseBody)) {
        errorMessage = Config.OBJECT_MAPPER.readValue(responseBody, Map.class);
      }
      if (MapUtils.isEmpty(errorMessage)) {
        throw new NonRetryableException(responseBody, response.getCode());
      }
      throw new NonRetryableException(errorMessage.toString(), response.getCode());
    }
  }

}
