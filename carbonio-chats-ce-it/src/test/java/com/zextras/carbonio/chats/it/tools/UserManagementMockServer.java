package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import java.util.concurrent.CompletableFuture;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;
import org.mockserver.model.Header;
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
      .withHeaders(
        Header.header("content-length", 0),
        Header.header("Connection", "Keep-Alive"),
        Header.header("User-Agent", "Apache-HttpClient/4.5.13 (Java/11.0.13)"),
        Header.header("Host", getHost())
      ).withKeepAlive(true)
      .withSecure(false);
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.ALL);
  }

  private String getHost() {
    return String.join(":", remoteAddress().getHostName(), Integer.toString(remoteAddress().getPort()));
  }

}
