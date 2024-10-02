
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;
import jakarta.ws.rs.Produces;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class HttpClientConfig {

  private final ConfigProperties config;

  @Produces
  @Default
  @Named("BobHttpClient")
  public CloseableHttpClient httpclient() {


    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(config.restClientMaxPoolSize());
    cm.setDefaultMaxPerRoute(config.restClientMaxPerRoute());

    RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(config.restClientConnectTimeout(), TimeUnit.SECONDS)
      .setResponseTimeout(config.restClientReadTimeout(), TimeUnit.SECONDS)
      .setRedirectsEnabled(true)
      .build();

    return HttpClients.custom()
      .setConnectionManager(cm)
      .setDefaultRequestConfig(requestConfig)
      .build();
  }

}
