// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

public class MongooseImMockServer extends ClientAndServer implements CloseableResource {

  public MongooseImMockServer(Integer... ports) {
    super(ports);
  }

  public MongooseImMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void verify(String method, String path, int iterationsNumber) {
    verify(method, path, null, iterationsNumber);
  }

  public void verify(String method, String path, Object body, int iterationsNumber) {
    HttpRequest request = getRequestDefinition(method, path, body);
    try {
      verify(request, VerificationTimes.exactly(iterationsNumber));
    } finally {
      clear(request, ClearType.LOG);
    }
  }

  public void setIsAliveResponse(boolean success) {
    HttpRequest request = request().withMethod("GET").withPath("/admin/commands");
    clear(request);
    when(request)
      .respond(
        response()
          .withStatusCode(success ? 200 : 500)
          .withContentType(MediaType.APPLICATION_JSON)
      );
  }


  public HttpRequest getRequestDefinition(String method, String path, @Nullable Object body) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path);
    Optional.ofNullable(body)
      .ifPresent(b -> request.withBody(JsonBody.json(b, MatchType.ONLY_MATCHING_FIELDS)));
    return request;
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping mongooseIM mock...");
    super.close();
  }
}
