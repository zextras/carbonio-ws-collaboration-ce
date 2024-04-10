// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.mockserver.verify.VerificationTimes;

public class VideoRecorderMockServer extends ClientAndServer implements CloseableResource {

  public VideoRecorderMockServer(Integer... ports) {
    super(ports);
  }

  public VideoRecorderMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void verify(
      String method,
      String path,
      @Nullable Map<String, String> queryParameters,
      int iterationsNumber) {
    HttpRequest request = request().withMethod(method).withPath(path);
    Optional.ofNullable(queryParameters)
        .ifPresent(parameters -> parameters.forEach(request::withQueryStringParameter));
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping Video Recorder mock...");
    super.close();
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

  public void mockRequestedResponse(
      String method, String path, String responseBody, boolean success) {
    HttpRequest request = getRequest(method, path);
    clear(request);
    when(request)
        .respond(
            response().withStatusCode(success ? 200 : 500).withBody(JsonBody.json(responseBody)));
  }
}
