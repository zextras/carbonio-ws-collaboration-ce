package com.zextras.carbonio.chats.it.tools;

import java.util.concurrent.CompletableFuture;
import org.mockserver.client.MockServerClient;

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
}
