package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.mockserver.verify.VerificationTimes;

public class RabbitMqMockServer extends ClientAndServer implements CloseableResource {

  public RabbitMqMockServer(Integer... ports) {
    super(ports);
  }

  public RabbitMqMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void verify(String method, String path, int iterationsNumber) {
    verify(method, path, null, iterationsNumber);
  }

  public void verify(String method, String path, Object body, int iterationsNumber) {
    HttpRequest request = getRequestDefinition(method, path, body);
    try {
      verify(request, VerificationTimes.exactly(iterationsNumber));
    } finally {
      clear(request, ClearType.LOG);
    }
  }

  public HttpRequest getRequestDefinition(String method, String path, @Nullable Object body) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path);
    Optional.ofNullable(body)
      .ifPresent(b -> request.withBody(JsonBody.json(b, MatchType.ONLY_MATCHING_FIELDS)));
    return request;
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping RabbitMQ mock...");
    super.close();
  }
}
