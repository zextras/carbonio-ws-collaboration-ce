package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

public class PreviewerMockServer extends MockServerClient {

  public PreviewerMockServer(CompletableFuture<Integer> portFuture) {
    super(portFuture);
  }

  public PreviewerMockServer(String host, int port) {
    super(host, port);
  }

  public PreviewerMockServer(String host, int port, String contextPath) {
    super(host, port, contextPath);
  }

  public void verify(String method, String path, @Nullable Map<String, String> queryParameters, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path);
    Optional.ofNullable(queryParameters).ifPresent(parameters ->
      parameters.forEach(request::withQueryStringParameter
      ));
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

}
