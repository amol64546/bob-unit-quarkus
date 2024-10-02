
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;

/**
 * This class is responsible for constructing URLs with path and query parameters.
 * It is annotated with @ApplicationScoped, meaning that a single instance will be
 * created and shared across the application.
 */
@ApplicationScoped
public class URLResolver {

  /**
   * Constructs a URL with the specified endpoint, query parameters, and path parameters.
   *
   * @param endpoint    the endpoint URL
   * @param queryParams the query parameters
   * @param pathParams  the path parameters
   * @return the constructed URL
   */
  public String constructUrl(String endpoint, Map<String, String> queryParams, Map<String, String> pathParams) {
    var urlBuilder = new StringBuilder(endpoint);

    // Resolve path parameters if any
    if (MapUtils.isNotEmpty(pathParams)) {
      pathParams.forEach((String paramName, String paramValue) -> {
        String placeholder = "{" + paramName + "}";
        // Replace placeholder with actual value in the endpoint
        int index = urlBuilder.indexOf(placeholder);
        while (index != -1) {
          urlBuilder.replace(index, index + placeholder.length(), paramValue);
          index = urlBuilder.indexOf(placeholder);
        }
      });
    }

    // Append query parameters to the URL
    if (MapUtils.isNotEmpty(queryParams)) {
      urlBuilder.append(constructQueryParams(queryParams));
    }

    return urlBuilder.toString();
  }

  /**
   * Constructs a query string from the specified query parameters.
   *
   * @param queryParams the query parameters
   * @return the constructed query string
   */
  private static String constructQueryParams(Map<String, String> queryParams) {
    if (MapUtils.isEmpty(queryParams)) {
      return "";
    }
    var queryString = new StringBuilder("?");
    queryParams.entrySet().forEach((Map.Entry<String, String> entry) ->
      queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&"));
    queryString.deleteCharAt(queryString.length() - 1);
    return queryString.toString();
  }
}
