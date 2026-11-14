// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

public class RabbitMqBroker implements CloseableResource {

  private static final String USER = "test";
  private static final String PASSWORD = "test";
  private static final int AMQP_PORT = 5672;
  private static final int MANAGEMENT_PORT = 15672;
  private static final int RECOVERY_INTERVAL_MILLIS = 200;

  private final GenericContainer<?> container =
      new GenericContainer<>("rabbitmq:4.2-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", USER)
          .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
          .withCopyToContainer(
              Transferable.of("deprecated_features.permit.transient_nonexcl_queues = false\n"),
              "/etc/rabbitmq/conf.d/90-like-rabbitmq-4.3.conf")
          .withExposedPorts(AMQP_PORT, MANAGEMENT_PORT)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));
  private final HttpClient http = HttpClient.newHttpClient();

  public RabbitMqBroker() {
    container.start();
    InMemoryConfigStore.set(ConfigName.EVENT_DISPATCHER_HOST, container.getHost());
    InMemoryConfigStore.set(
        ConfigName.EVENT_DISPATCHER_PORT, String.valueOf(container.getMappedPort(AMQP_PORT)));
    InMemoryConfigStore.set(ConfigName.EVENT_DISPATCHER_USER_USERNAME, USER);
    InMemoryConfigStore.set(ConfigName.EVENT_DISPATCHER_USER_PASSWORD, PASSWORD);
    InMemoryConfigStore.set(
        ConfigName.NETWORK_RECOVERY_INTERVAL_IN_MILLI, String.valueOf(RECOVERY_INTERVAL_MILLIS));
  }

  public ConnectionFactory connectionFactory() {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(container.getHost());
    factory.setPort(container.getMappedPort(AMQP_PORT));
    factory.setUsername(USER);
    factory.setPassword(PASSWORD);
    factory.setNetworkRecoveryInterval(RECOVERY_INTERVAL_MILLIS);
    return factory;
  }

  public String rabbitmqctl(String... args) throws IOException, InterruptedException {
    return container
        .execInContainer(
            Stream.concat(Stream.of("rabbitmqctl"), Stream.of(args)).toArray(String[]::new))
        .getStdout();
  }

  public List<String> queues() throws IOException, InterruptedException {
    return rabbitmqctl("list_queues", "--quiet", "--no-table-headers", "name")
        .lines()
        .map(String::strip)
        .toList();
  }

  public void reset() throws IOException, InterruptedException {
    management("DELETE", "/api/vhosts/%2F", "");
    management("PUT", "/api/vhosts/%2F", "");
    management(
        "PUT",
        "/api/permissions/%2F/" + USER,
        "{\"configure\":\".*\",\"write\":\".*\",\"read\":\".*\"}");
  }

  private void management(String method, String path, String json)
      throws IOException, InterruptedException {
    String credentials = USER + ":" + PASSWORD;
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(
                    "http://"
                        + container.getHost()
                        + ":"
                        + container.getMappedPort(MANAGEMENT_PORT)
                        + path))
            .header(
                "Authorization",
                "Basic "
                    + Base64.getEncoder()
                        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)))
            .header("Content-Type", "application/json")
            .method(method, BodyPublishers.ofString(json))
            .build();
    int status = http.send(request, BodyHandlers.discarding()).statusCode();
    if (status >= 300) throw new IllegalStateException(method + " " + path + " answered " + status);
  }

  @Override
  public void close() {
    container.stop();
  }
}
