package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.chats.it.Utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import com.zextras.carbonio.chats.mongooseim.admin.model.AddcontactDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.AffiliationDetailsDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.AffiliationDetailsDto.AffiliationEnum;
import com.zextras.carbonio.chats.mongooseim.admin.model.InviteDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.RoomDetailsDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.SubscriptionActionDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.SubscriptionActionDto.ActionEnum;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameter;

public class MongooseIMExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(MongooseIMExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "mongoose_client";
  private final static int       PORT                = 12345;
  private static final String    HOST                = "localhost";

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(CLIENT_STORE_ENTRY, (key) -> {
      ChatsLogger.debug("Starting MongooseIM client mock...");
      MongooseImMockServer client = new MongooseImMockServer(PORT);
      mockResponses(client);
      InMemoryConfigStore.set(ConfigName.XMPP_SERVER_HOST, HOST);
      InMemoryConfigStore.set(ConfigName.XMPP_SERVER_HTTP_PORT, Integer.toString(PORT));
      return client;
    }, MongooseImMockServer.class);
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
      return Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(mock -> (MongooseImMockServer) mock)
      .ifPresent(
        mock -> mock.clear(request(), ClearType.LOG)
      );
  }

  private void mockResponses(MockServerClient client) {
    List<String> roomsIds = List.of(
      "86cc37de-1217-4056-8c95-69997a6bccce",
      "b7774109-15ba-4889-b480-2dcf952d0991",
      "c9f83f1c-9b96-4731-9404-79e45a5d6d3c");
    List<MockUserProfile> accounts = MockedAccount.getAccounts();
    mockIsAlive(client);
    mockDeleteRoom(client);
    mockSendMessageToRoom(client);
    mockStanzasMessage(client);
    accounts.forEach(account -> {
      roomsIds.forEach(roomId -> mockCreateRoom(client, roomId, account.getUUID()));
      accounts.forEach(account2 -> mockAddRoomMember(client, account.getUUID(), account2.getUUID()));
      mockRemoveRoomMember(client, account.getId());
      mockPromoteAndDemoteMember(client, account.getId());
    });
    List<String> userIds = List.of(
      MockedAccount.getAccount(MockedAccountType.SNOOPY).getId(),
      MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getId());
    userIds.forEach(user1id ->
      userIds.forEach(user2id -> mockContactSubscription(client, user1id, user2id))
    );
  }

  private void mockCreateRoom(MockServerClient client, String roomId, UUID senderId) {
    client.when(
      request()
        .withMethod("PUT")
        .withPath("/admin/muc-lights/carbonio")
        .withBody(JsonBody.json(new RoomDetailsDto()
          .id(roomId)
          .owner(String.format("%s@carbonio", senderId))
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
        .withPath("/admin/muc-lights/carbonio/{roomId}/participants")
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withBody(JsonBody.json(new InviteDto()
          .sender(String.format("%s@carbonio", senderId))
          .recipient(String.format("%s@carbonio", recipientId))))
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockContactSubscription(MockServerClient client, String user1id, String user2id) {
    if (user1id.equals(user2id)) {
      return;
    }
    client.when(
      request()
        .withMethod("PUT")
        .withPath("/admin/contacts/{user}/{contact}/manage")
        .withPathParameter(Parameter.param("user", String.format("%s%scarbonio", user1id, "%40")))
        .withPathParameter(Parameter.param("contact", String.format("%s%scarbonio", user2id, "%40")))
        .withBody(JsonBody.json(new SubscriptionActionDto().action(ActionEnum.CONNECT)))
    ).respond(
      response()
        .withStatusCode(204)
    );
  }

  private void mockRemoveRoomMember(MockServerClient client, String memberToRemove) {
    client.when(
      request()
        .withMethod("PUT")
        .withPath("/admin/muc-lights/carbonio/{roomId}/{userId}/affiliation")
        .withBody(JsonBody.json(new AffiliationDetailsDto().target(String.format("%s@carbonio", memberToRemove))
          .affiliation(AffiliationEnum.NONE)))
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withPathParameter(Parameter.param("userId", ".*"))
    ).respond(
      response()
        .withStatusCode(204)
    );
  }

  private void mockPromoteAndDemoteMember(MockServerClient client, String memberToUpdate) {
    client.when(
      request()
        .withMethod("PUT")
        .withPath("/admin/muc-lights/carbonio/{roomId}/{userId}/affiliation")
        .withBody(JsonBody.json(new AffiliationDetailsDto().target(String.format("%s@carbonio", memberToUpdate))
          .affiliation(AffiliationEnum.OWNER)))
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withPathParameter(Parameter.param("userId", ".*"))
    ).respond(
      response()
        .withStatusCode(204)
    );
    client.when(
      request()
        .withMethod("PUT")
        .withPath("/admin/muc-lights/carbonio/{roomId}/{userId}/affiliation")
        .withBody(JsonBody.json(new AffiliationDetailsDto().target(String.format("%s@carbonio", memberToUpdate))
          .affiliation(AffiliationEnum.MEMBER)))
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withPathParameter(Parameter.param("userId", ".*"))
    ).respond(
      response()
        .withStatusCode(204)
    );
  }

  private void mockSendMessageToRoom(MockServerClient client) {
    client.when(
      request()
        .withMethod("POST")
        .withPath("/admin/muc-lights/carbonio/{roomId}/messages")
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
        .withPath("/admin/commands")
    ).respond(
      response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
    );
  }

  private void mockDeleteRoom(MockServerClient client) {
    client.when(
      request()
        .withMethod("DELETE")
        .withPath("/admin/muc-lights/carbonio/{roomId}/{userId}/management")
        .withPathParameter(Parameter.param("roomId", ".*"))
        .withPathParameter(Parameter.param("userId", ".*"))
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockStanzasMessage(MockServerClient client) {
    client.when(
      request()
        .withMethod("POST")
        .withPath("/admin/stanzas")
    ).respond(
      response()
        .withStatusCode(201)
    );

  }
}
