package com.zextras.carbonio.chats.it.tools;

import java.util.concurrent.CompletableFuture;
import org.mockserver.client.MockServerClient;

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
}
