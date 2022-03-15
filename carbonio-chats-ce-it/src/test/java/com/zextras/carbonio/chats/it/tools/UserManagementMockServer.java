package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

public class UserManagementMockServer extends ClientAndServer implements CloseableResource {

  public UserManagementMockServer(Integer... ports) {
    super(ports);
  }

  public UserManagementMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void verify(String method, String path, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path);
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  public void verify(String method, String path, @Nullable String cookies, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path);
    Optional.ofNullable(cookies)
      .ifPresent(c -> request.withHeaders(header("Cookie", String.format("ZM_AUTH_TOKEN=%s", c))));

    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping user management mock...");
    super.close();
  }
}
