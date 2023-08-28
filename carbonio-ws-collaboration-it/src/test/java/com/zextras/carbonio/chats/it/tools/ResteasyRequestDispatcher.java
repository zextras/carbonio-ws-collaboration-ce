// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.SynchronousExecutionContext;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.Registry;

public class ResteasyRequestDispatcher {

  private final Dispatcher dispatcher;

  public ResteasyRequestDispatcher(Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  public Registry getRegistry() {
    return dispatcher.getRegistry();
  }

  public MockHttpResponse get(String url) throws URISyntaxException {
    return get(url, null);
  }

  public MockHttpResponse get(String url, @Nullable String userToken) throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get(url);
    Optional.ofNullable(userToken).ifPresent(token -> request.cookie("ZM_AUTH_TOKEN", token));

    return sendRequest(request);
  }

  private MockHttpRequest preparePost(String path, @Nullable Map<String, String> requestHeaders, @Nullable String userToken)
    throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.post(path);
    Optional.ofNullable(requestHeaders).ifPresent(r -> r.forEach(request::header));
    Optional.ofNullable(userToken).ifPresent(token -> {
      request.cookie("ZM_AUTH_TOKEN", token);
      request.header("Cookie", token);
    });
    request.accept(MediaType.APPLICATION_JSON);
    request.contentType(MediaType.APPLICATION_JSON_TYPE);
    return request;
  }

  public MockHttpResponse post(String path, String userToken) throws URISyntaxException {
    return post(path, (String)null, Map.of(), userToken);
  }

  public MockHttpResponse post(String path, String requestBody, @Nullable String userToken) throws URISyntaxException {
    return post(path, requestBody, Map.of(), userToken);
  }

  public MockHttpResponse post(
    String path, @Nullable String requestBody, Map<String, String> requestHeaders, @Nullable String userToken
  ) throws URISyntaxException {
    MockHttpRequest request = preparePost(path, requestHeaders, userToken);
    Optional.ofNullable(requestBody).ifPresent(body -> request.content(requestBody.getBytes()));
    return sendRequest(request);
  }

  public MockHttpResponse post(
    String path, byte[] requestBody, Map<String, String> requestHeaders, @Nullable String userToken
  ) throws URISyntaxException {
    return sendRequest(preparePost(path, requestHeaders, userToken).content(requestBody));
  }


  private MockHttpRequest preparePut(String path, Map<String, String> requestHeaders, @Nullable String userToken)
    throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.put(path);
    requestHeaders.forEach(request::header);
    Optional.ofNullable(userToken).ifPresent(token -> request.cookie("ZM_AUTH_TOKEN", token));

    request.accept(MediaType.APPLICATION_JSON);
    request.contentType(MediaType.APPLICATION_JSON_TYPE);
    return request;
  }

  public MockHttpResponse put(String path, @Nullable String requestBody, @Nullable String userToken)
    throws URISyntaxException {
    return put(path, requestBody, Map.of(), userToken);
  }

  public MockHttpResponse put(
    String path, @Nullable String requestBody, Map<String, String> requestHeaders, @Nullable String userToken
  )
    throws URISyntaxException {
    MockHttpRequest request = preparePut(path, requestHeaders, userToken);
    Optional.ofNullable(requestBody).ifPresent(body -> request.content(requestBody.getBytes()));
    return sendRequest(request);
  }

  public MockHttpResponse put(
    String path, byte[] requestBody, Map<String, String> requestHeaders, @Nullable String userToken
  )
    throws URISyntaxException {
    return sendRequest(preparePut(path, requestHeaders, userToken).content(requestBody));
  }

  private MockHttpRequest prepareDelete(String path, Map<String, String> requestHeaders, @Nullable String userToken)
    throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.delete(path);
    requestHeaders.forEach(request::header);
    Optional.ofNullable(userToken).ifPresent(token -> request.cookie("ZM_AUTH_TOKEN", token));

    request.accept(MediaType.APPLICATION_JSON);
    request.contentType(MediaType.APPLICATION_JSON_TYPE);
    return request;
  }

  public MockHttpResponse delete(String path, @Nullable String userToken) throws URISyntaxException {
    return sendRequest(prepareDelete(path, Map.of(), userToken));
  }

  public MockHttpResponse delete(String path, Map<String, String> requestHeaders, @Nullable String userToken)
    throws URISyntaxException {
    return sendRequest(prepareDelete(path, requestHeaders, userToken));
  }

  private MockHttpResponse sendRequest(MockHttpRequest request) {
    MockHttpResponse response = new MockHttpResponse();
    SynchronousExecutionContext synchronousExecutionContext = new SynchronousExecutionContext(
      (SynchronousDispatcher) dispatcher, request, response);
    request.setAsynchronousContext(synchronousExecutionContext);
    dispatcher.invoke(request, response);
    return response;
  }
}
