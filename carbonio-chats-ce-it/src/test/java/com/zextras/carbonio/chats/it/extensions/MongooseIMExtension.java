package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedAccount.MockAccount;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import com.zextras.carbonio.chats.mongooseim.admin.model.InviteDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.RoomDetailsDto;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.Parameter;
import org.mockserver.netty.MockServer;

public class MongooseIMExtension implements AfterAllCallback, BeforeAllCallback, ParameterResolver {

  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(MongooseIMExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "client";
  private final static String    SERVER_STORE_ENTRY  = "server";
  private final static int       PORT                = 12345;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Instant startTime = Instant.now();
    ChatsLogger.debug("Starting MongooseIM Mockserver...");
    MockServer mockServer = new MockServer(PORT);
    ChatsLogger.debug("Starting MongooseIM client mock...");
    MockServerClient mockClient = new MongooseImMockServer("localhost", PORT);
    mockResponses(mockClient);

    InMemoryConfigStore.set("MONGOOSEIM_CLIENT_REST_BASE_URL", httpUrlFromMockClient(mockClient));
    InMemoryConfigStore.set("MONGOOSEIM_ADMIN_REST_BASE_URL", httpUrlFromMockClient(mockClient));
    context.getStore(EXTENSION_NAMESPACE).put(CLIENT_STORE_ENTRY, mockClient);
    context.getStore(EXTENSION_NAMESPACE).put(SERVER_STORE_ENTRY, mockServer);
    ChatsLogger.debug(
      "Mongoose extension startup took " + TimeUtils.durationToString(Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(objectMockClient -> (MongooseImMockServer) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping MongooseIM client mock...");
        client.stop(true);
      });
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(SERVER_STORE_ENTRY))
      .map(objectMockClient -> (MockServer) objectMockClient)
      .ifPresent(server -> {
        ChatsLogger.debug("Stopping MongooseIM Mockserver...");
        server.stop();
      });
  }

  private String httpUrlFromMockClient(MockServerClient mockServerClient) {
    StringBuilder stringBuilder = new StringBuilder();
    if (mockServerClient.isSecure()) {
      stringBuilder.append("https://");
    } else {
      stringBuilder.append("http://");
    }
    stringBuilder.append(mockServerClient.remoteAddress().getHostString());
    stringBuilder.append(":");
    stringBuilder.append(mockServerClient.remoteAddress().getPort());
    stringBuilder.append("/");
    stringBuilder.append(mockServerClient.contextPath());
    return stringBuilder.toString();
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(MongooseImMockServer.class);
  }

  @Override
  public Object resolveParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(MongooseImMockServer.class)) {
      return Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  private void mockResponses(MockServerClient client) {
    String room1Id = "86cc37de-1217-4056-8c95-69997a6bccce";
    String room2Id = "b7774109-15ba-4889-b480-2dcf952d0991";
    List<String> roomsIds = List.of(room1Id, room2Id);
    List<MockAccount> accounts = MockedAccount.getAccounts();
    mockIsAlive(client);
    mockRemoveRoomMember(client);
    mockDeleteRoom(client);
    mockSendMessageToRoom(client);
    accounts.forEach(account -> {
      roomsIds.forEach(roomId -> mockCreateRoom(client, roomId, account.getUUID()));
      accounts.forEach(account2 ->
        mockAddRoomMember(client, account.getUUID(), account2.getUUID())
      );
    });
  }

  private void mockCreateRoom(MockServerClient client, String roomId, UUID senderId) {
    client.when(
      request()
        .withMethod("PUT")
        .withPath("/muc-lights/localhost")
        .withBody(JsonBody.json(new RoomDetailsDto()
          .id(roomId)
          .owner(String.format("%s@localhost", senderId))
          .name(roomId)
          .subject("Test room")))
    ).respond(
      response()
        .withHeader(header("Content-Type", "application/json"))
        .withStatusCode(200)
    );
  }

  private void mockAddRoomMember(MockServerClient client, UUID senderId, UUID recipientId) {
    if (senderId.equals(recipientId)) {
      return;
    }
    client.when(
      request()
        .withMethod("POST")
        .withPath("/muc-lights/localhost/{roomId}/participants")
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withBody(JsonBody.json(new InviteDto()
          .sender(String.format("%s@localhost", senderId))
          .recipient(String.format("%s@localhost", recipientId))))
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockRemoveRoomMember(MockServerClient client) {
    client.when(
      request()
        .withMethod("DELETE")
        .withPath("/rooms/{roomId}/users/{userId}")
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withPathParameter(Parameter.param("userId", ".*"))
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockSendMessageToRoom(MockServerClient client) {
    client.when(
      request()
        .withMethod("POST")
        .withPath("/muc-lights/localhost/{roomId}/messages")
        .withPathParameter("roomId", ".*")
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockIsAlive(MockServerClient client) {
    client.when(
      request()
        .withMethod("GET")
        .withPath("/commands")
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockDeleteRoom(MockServerClient client) {
    client.when(
      request()
        .withMethod("DELETE")
        .withPath("/muc-lights/localhost/{roomId}/{userId}/management")
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withPathParameter(Parameter.param("userId", ".*"))
    ).respond(
      response()
        .withStatusCode(200)
    );
  }
}
