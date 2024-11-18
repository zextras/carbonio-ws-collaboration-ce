// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;

public class VideoServerMockServer extends ClientAndServer implements CloseableResource {

  public VideoServerMockServer(Integer... ports) {
    super(ports);
  }

  public VideoServerMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public HttpRequest getRequest(String method, String path, String body) {
    return request()
        .withMethod(method)
        .withPath(path)
        .withHeader(Header.header("content-type", "application/json"))
        .withBody(JsonBody.json(body));
  }

  public HttpRequest getRequest(String method, String path) {
    return request()
        .withMethod(method)
        .withPath(path)
        .withHeader(Header.header("content-type", "application/json"));
  }

  public HttpRequest getIsAliveRequest() {
    return getRequest("GET", "/janus/info");
  }

  public void setIsAliveResponse(boolean success) {
    mockRequestedResponse("GET", "/janus/info", "{\"janus\":\"server_info\"}", success);
  }

  public void mockRequestedResponse(
      String method, String path, String requestBody, String responseBody, boolean success) {
    HttpRequest request = getRequest(method, path, requestBody);
    clear(request);
    when(request)
        .respond(
            response().withStatusCode(success ? 200 : 500).withBody(JsonBody.json(responseBody)));
  }

  public void mockRequestedResponse(
      String method, String path, String responseBody, boolean success) {
    HttpRequest request = getRequest(method, path);
    clear(request);
    when(request)
        .respond(
            response().withStatusCode(success ? 200 : 500).withBody(JsonBody.json(responseBody)));
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping Video Server mock...");
    super.close();
  }
}
