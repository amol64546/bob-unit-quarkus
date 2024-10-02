
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import com.aidtaas.mobius.unit.exception.BobRestClientResponseMapper;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * This interface defines the methods for making REST API calls.
 * It is annotated with @RegisterRestClient, which registers it as a REST client with the MicroProfile Rest Client.
 */
@Path("")
@RegisterRestClient(configKey = "base-url")
@RegisterProvider(value = BobRestClientResponseMapper.class)
public interface GenericRestClient {

  /**
   * Makes a POST request to the specified endpoint.
   *
   * @param payload the request body
   * @return the response from the server
   */
  @POST
  @Path("")
  Response invokePostEndpoint(Object payload);

  /**
   * Makes a GET request to the specified endpoint.
   *
   * @return the response from the server
   */
  @GET
  @Path("")
  Response invokeGetEndpoint();

  /**
   * Makes a PUT request to the specified endpoint.
   *
   * @param payload the request body
   * @return the response from the server
   */
  @PUT
  @Path("")
  Response invokePutEndpoint(Object payload);

  /**
   * Makes a DELETE request to the specified endpoint.
   *
   * @return the response from the server
   */
  @DELETE
  @Path("")
  Response invokeDeleteEndpoint();

}
