package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

public class StorageMockServer extends ClientAndServer implements CloseableResource {

  public StorageMockServer(Integer... ports) {
    super(ports);
  }

  public StorageMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void verify(String method, String path, String node, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path)
      .withQueryStringParameter("node", node)
      .withQueryStringParameter("type", "chats");

    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping Storages mock...");
    super.close();
  }
}
