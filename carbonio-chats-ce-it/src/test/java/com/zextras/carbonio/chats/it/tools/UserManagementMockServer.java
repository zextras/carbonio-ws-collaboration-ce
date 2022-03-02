package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

public class UserManagementMockServer extends MockServerClient {

  public UserManagementMockServer(CompletableFuture<Integer> portFuture) {
    super(portFuture);
  }

  public UserManagementMockServer(String host, int port) {
    super(host, port);
  }

  public UserManagementMockServer(String host, int port, String contextPath) {
    super(host, port, contextPath);
  }

  public void verify(String method, String path, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path)
      .withSecure(false); //TODO why if I remove this, tests fail?
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.ALL);
  }

  public void verify(String method, String path, @Nullable String cookies, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path)
      .withSecure(false); //TODO why if I remove this, tests fail?
    Optional.ofNullable(cookies).ifPresent(c -> request.withHeaders(header("Cookie", c)));

    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.ALL);
  }

}
