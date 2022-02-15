package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import java.util.concurrent.CompletableFuture;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

public class StorageMockServer extends MockServerClient {

  public StorageMockServer(CompletableFuture<Integer> portFuture) {
    super(portFuture);
  }

  public StorageMockServer(String host, int port) {
    super(host, port);
  }

  public StorageMockServer(String host, int port, String contextPath) {
    super(host, port, contextPath);
  }

  public void verify(String method, String path, String node, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path)
      .withQueryStringParameter("node", node)
      .withQueryStringParameter("type", "chats")
      .withHeaders(
        Header.header("content-length", "0"),
        Header.header("Connection", "Keep-Alive"),
        Header.header("User-Agent", "okhttp/3.14.9"),
        Header.header("Host", getHost()),
        Header.header("Accept-Encoding", "gzip")
      )
      .withKeepAlive(true)
      .withSecure(false);
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.ALL);
  }

  private String getHost() {
    return String.join(":", remoteAddress().getHostName(), Integer.toString(remoteAddress().getPort()));
  }

}
