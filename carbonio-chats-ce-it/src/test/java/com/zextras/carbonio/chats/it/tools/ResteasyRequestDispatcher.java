package com.zextras.carbonio.chats.it.tools;

import java.net.URISyntaxException;
import java.util.Map;
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

  public MockHttpResponse sendGet(String url) throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get(url);
    MockHttpResponse response = new MockHttpResponse();
    SynchronousExecutionContext synchronousExecutionContext = new SynchronousExecutionContext(
      (SynchronousDispatcher) dispatcher, request, response);
    request.setAsynchronousContext(synchronousExecutionContext);
    return sendHttpRequest(request, response);
  }

  public MockHttpResponse sendPost(String path, String requestBody, Map<String, String> requestHeaders)
    throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.post(path);
    request.accept(MediaType.APPLICATION_JSON);
    request.contentType(MediaType.APPLICATION_JSON_TYPE);
    request.content(requestBody.getBytes());

    MockHttpResponse response = new MockHttpResponse();
    SynchronousExecutionContext synchronousExecutionContext = new SynchronousExecutionContext(
      (SynchronousDispatcher) dispatcher, request, response);
    request.setAsynchronousContext(synchronousExecutionContext);
    return sendHttpRequest(request, response);
  }

  public MockHttpResponse sendPut(String path, String requestBody, Map<String, String> requestHeaders)
    throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.put(path);
    request.accept(MediaType.APPLICATION_JSON);
    request.contentType(MediaType.APPLICATION_JSON_TYPE);
    request.content(requestBody.getBytes());
    MockHttpResponse response = new MockHttpResponse();
    SynchronousExecutionContext synchronousExecutionContext = new SynchronousExecutionContext(
      (SynchronousDispatcher) dispatcher, request, response);
    request.setAsynchronousContext(synchronousExecutionContext);
    return sendHttpRequest(request, response);
  }

  public MockHttpResponse sendDelete(String path) throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.delete(path);
    MockHttpResponse response = new MockHttpResponse();
    SynchronousExecutionContext synchronousExecutionContext = new SynchronousExecutionContext(
      (SynchronousDispatcher) dispatcher, request, response);
    request.setAsynchronousContext(synchronousExecutionContext);
    return sendHttpRequest(request, response);
  }

  private MockHttpResponse sendHttpRequest(MockHttpRequest request, MockHttpResponse response)
    throws URISyntaxException {
    dispatcher.invoke(request, response);
    return response;
  }
}
