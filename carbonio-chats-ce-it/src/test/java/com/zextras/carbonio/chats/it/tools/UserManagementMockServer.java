package com.zextras.carbonio.chats.it.tools;

import java.util.concurrent.CompletableFuture;
import org.mockserver.client.MockServerClient;

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
}
