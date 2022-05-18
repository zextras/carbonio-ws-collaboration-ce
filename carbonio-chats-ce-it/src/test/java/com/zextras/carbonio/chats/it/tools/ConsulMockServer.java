package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

public class ConsulMockServer extends ClientAndServer implements CloseableResource {

  public ConsulMockServer(Integer... ports) {
    super(ports);
  }

  public ConsulMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
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

  @Override
  public void close() {
    ChatsLogger.debug("Stopping Consul mock...");
    super.close();
  }

}
