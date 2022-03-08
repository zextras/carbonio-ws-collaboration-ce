package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.mockserver.verify.VerificationTimes;

public class MongooseImMockServer extends MockServerClient {

  public MongooseImMockServer(CompletableFuture<Integer> portFuture) {
    super(portFuture);
  }

  public MongooseImMockServer(String host, int port) {
    super(host, port);
  }

  public MongooseImMockServer(String host, int port, String contextPath) {
    super(host, port, contextPath);
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

  public HttpRequest getRequestDefinition(String method, String path, @Nullable Object body) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path);
    Optional.ofNullable(body)
      .ifPresent(b -> request.withBody(JsonBody.json(b, MatchType.ONLY_MATCHING_FIELDS)));
    return request;
  }
}
