// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.MessageDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.http.entity.ContentType;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.mockserver.verify.VerificationTimes;

@ApiIntegrationTest
class SendIdempotencyIT {

  private static final String MESSAGES =
      "SELECT count(*) AS n FROM chats.message WHERE room_id = :roomId";
  private static final String CLAIMS =
      "SELECT count(*) AS n FROM chats.message_send_dedup WHERE room_id = :roomId";

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final Channel channel;
  private final Database database;
  private final StorageMockServer storageMockServer;

  private final UUID sender = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
  private final String senderToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
  private final UUID member = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
  private final String memberToken =
      MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
  private final String outsiderToken =
      MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getToken();

  private UUID roomId;

  public SendIdempotencyIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      Channel channel,
      Database database,
      StorageMockServer storageMockServer) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.channel = channel;
    this.database = database;
    this.storageMockServer = storageMockServer;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @BeforeEach
  void setUp() {
    roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "idempotency", List.of(sender, member));
  }

  MockHttpResponse send(String token, Map<String, String> body) throws Exception {
    return dispatcher.post(
        String.format("/rooms/%s/messages", roomId), objectMapper.writeValueAsString(body), token);
  }

  MessageDto created(MockHttpResponse response) throws Exception {
    assertEquals(201, response.getStatus(), response.getContentAsString());
    return objectMapper.readValue(response.getContentAsString(), MessageDto.class);
  }

  long count(String sql) {
    return database.sqlQuery(sql).setParameter("roomId", roomId.toString()).findOne().getLong("n");
  }

  String claimOf(UUID user, String tempId) {
    return database
        .sqlQuery(
            "SELECT message_id FROM chats.message_send_dedup"
                + " WHERE room_id = :roomId AND sender_id = :userId AND temp_id = :tempId")
        .setParameter("roomId", roomId.toString())
        .setParameter("userId", user.toString())
        .setParameter("tempId", tempId)
        .findOneOrEmpty()
        .map(row -> row.getString("message_id"))
        .orElse(null);
  }

  void ageClaims(String age) {
    database
        .sqlUpdate(
            "UPDATE chats.message_send_dedup SET created_at = created_at - CAST(:age AS interval)"
                + " WHERE room_id = :roomId")
        .setParameter("age", age)
        .setParameter("roomId", roomId.toString())
        .execute();
  }

  String bindQueue(UUID user) throws Exception {
    channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
    String queue = channel.queueDeclare().getQueue();
    channel.queueBind(queue, user.toString(), "user-events");
    return queue;
  }

  JsonNode nextEvent(String queue) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      GetResponse response = channel.basicGet(queue, true);
      if (response != null) {
        return objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
      }
      Thread.onSpinWait();
    }
    throw new AssertionError("no WS event received on " + queue);
  }

  void assertNextEventIsTheSentinel(String queue) throws Exception {
    created(send(senderToken, Map.of("text", "sentinel")));
    JsonNode event = nextEvent(queue);
    assertEquals("MessageReceived", event.get("type").asText());
    assertEquals("sentinel", event.get("message").get("text").asText());
  }

  @Nested
  @DisplayName("Text send")
  class TextSend {

    @Test
    @DisplayName("The same tempId twice: one row, one event, the original answered twice")
    void sameTempIdTwice() throws Exception {
      String queue = bindQueue(member);
      MessageDto first = created(send(senderToken, Map.of("text", "hello", "tempId", "t-1")));
      JsonNode event = nextEvent(queue);
      assertEquals("MessageReceived", event.get("type").asText());
      assertEquals(first.getId().toString(), event.get("message").get("id").asText());

      MessageDto second = created(send(senderToken, Map.of("text", "hello", "tempId", "t-1")));

      assertEquals(first.getId(), second.getId());
      assertEquals(first.getCreatedAt().toInstant(), second.getCreatedAt().toInstant());
      assertEquals("hello", second.getText());
      assertEquals(1, count(MESSAGES));
      assertEquals(first.getId().toString(), claimOf(sender, "t-1"));
      assertNextEventIsTheSentinel(queue);
    }

    @Test
    @DisplayName("A tempId older than one hour creates a new message")
    void expiredTempId() throws Exception {
      MessageDto first = created(send(senderToken, Map.of("text", "one", "tempId", "t-2")));
      ageClaims("61 minutes");

      MessageDto second = created(send(senderToken, Map.of("text", "two", "tempId", "t-2")));

      assertNotEquals(first.getId(), second.getId());
      assertEquals(2, count(MESSAGES));
      assertEquals(second.getId().toString(), claimOf(sender, "t-2"));
    }

    @Test
    @DisplayName("The same tempId from another sender creates another message")
    void sameTempIdOtherSender() throws Exception {
      MessageDto mine = created(send(senderToken, Map.of("text", "mine", "tempId", "t-3")));
      MessageDto theirs = created(send(memberToken, Map.of("text", "theirs", "tempId", "t-3")));

      assertNotEquals(mine.getId(), theirs.getId());
      assertEquals(2, count(MESSAGES));
      assertEquals(2, count(CLAIMS));
    }

    @Test
    @DisplayName("Without a tempId, or with a blank one, every send creates a message")
    void noTempId() throws Exception {
      created(send(senderToken, Map.of("text", "a")));
      created(send(senderToken, Map.of("text", "a")));
      created(send(senderToken, Map.of("text", "a", "tempId", "  ")));
      created(send(senderToken, Map.of("text", "a", "tempId", "  ")));

      assertEquals(4, count(MESSAGES));
      assertEquals(0, count(CLAIMS));
    }

    @Test
    @DisplayName("A non-member gets 403 and claims nothing")
    void outsider() throws Exception {
      assertEquals(403, send(outsiderToken, Map.of("text", "x", "tempId", "t-4")).getStatus());
      assertEquals(0, count(CLAIMS));
    }

    @Test
    @DisplayName("An unknown reply target gets 404 and leaves the tempId free")
    void unknownReplyTarget() throws Exception {
      assertEquals(
          404,
          send(
                  senderToken,
                  Map.of("text", "x", "tempId", "t-5", "replyToId", UUID.randomUUID().toString()))
              .getStatus());
      assertEquals(0, count(CLAIMS));

      created(send(senderToken, Map.of("text", "x", "tempId", "t-5")));
      assertEquals(1, count(MESSAGES));
    }

    @Test
    @DisplayName("A retry after an edit answers the message in its current state")
    void retryAfterEdit() throws Exception {
      MessageDto first = created(send(senderToken, Map.of("text", "v1", "tempId", "t-6")));
      assertEquals(
          200,
          dispatcher
              .put(
                  String.format("/rooms/%s/messages/%s/edit", roomId, first.getId()),
                  objectMapper.writeValueAsString(Map.of("text", "v2")),
                  senderToken)
              .getStatus());

      MessageDto retried = created(send(senderToken, Map.of("text", "v1", "tempId", "t-6")));

      assertEquals(first.getId(), retried.getId());
      assertEquals("v2", retried.getText());
      assertNotNull(retried.getEditedInfo());
    }

    @Test
    @DisplayName("A tempId claimed by a request still running answers 409")
    void claimInFlight() throws Exception {
      database
          .sqlUpdate(
              "INSERT INTO chats.message_send_dedup"
                  + " (room_id, sender_id, temp_id, message_id, created_at)"
                  + " VALUES (:roomId, :userId, 't-7', :messageId,"
                  + " CAST(clock_timestamp() AS timestamp))")
          .setParameter("roomId", roomId.toString())
          .setParameter("userId", sender.toString())
          .setParameter("messageId", UUID.randomUUID().toString())
          .execute();

      assertEquals(409, send(senderToken, Map.of("text", "x", "tempId", "t-7")).getStatus());
      assertEquals(0, count(MESSAGES));
    }

    @Test
    @DisplayName("A tempId longer than 64 characters is rejected before any write")
    void tempIdTooLong() throws Exception {
      assertEquals(
          400, send(senderToken, Map.of("text", "x", "tempId", "t".repeat(65))).getStatus());
      assertEquals(0, count(MESSAGES));
      assertEquals(0, count(CLAIMS));
    }
  }

  @Nested
  @DisplayName("Attachment upload")
  class AttachmentUpload {

    private final HttpRequest anyUpload =
        request()
            .withMethod("PUT")
            .withPath("/upload")
            .withQueryStringParameter(param("type", "chats"));
    private FileMock fileMock;

    @BeforeEach
    void resetStorage() {
      fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      storageMockServer.clear(anyUpload);
    }

    @AfterEach
    void clearStorage() {
      storageMockServer.clear(anyUpload);
    }

    private void uploadsAnswer(int status, Times times) {
      storageMockServer
          .when(anyUpload, times)
          .respond(
              response()
                  .withStatusCode(status)
                  .withBody(
                      JsonBody.json(
                          new StorageMockServer.UploadResponse()
                              .digest("")
                              .digestAlgorithm("")
                              .size(fileMock.getSize()))));
    }

    private MockHttpResponse upload(String tempId) throws Exception {
      return dispatcher.postMultipart(
          String.format("/rooms/%s/attachments", roomId),
          fileMock.getFileBytes(),
          Map.of("contentLength", String.valueOf(fileMock.getSize()), "tempId", tempId),
          StringFormatUtils.encodeToUtf8(fileMock.getName()),
          ContentType.parse(fileMock.getMimeType()),
          senderToken);
    }

    @Test
    @DisplayName(
        "A retry while the first upload is storing answers 409; the claim is committed before"
            + " storage")
    void uploadInFlight() throws Exception {
      CountDownLatch entered = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      storageMockServer
          .when(anyUpload, Times.once())
          .respond(
              req -> {
                entered.countDown();
                release.await(30, TimeUnit.SECONDS);
                return response()
                    .withStatusCode(201)
                    .withBody(
                        JsonBody.json(
                            new StorageMockServer.UploadResponse()
                                .digest("")
                                .digestAlgorithm("")
                                .size(fileMock.getSize())));
              });
      ExecutorService pool = Executors.newSingleThreadExecutor();
      try {
        Future<MockHttpResponse> first = pool.submit(() -> upload("att-4"));
        assertTrue(entered.await(30, TimeUnit.SECONDS));
        String claimed = claimOf(sender, "att-4");
        assertNotNull(claimed);
        assertEquals(409, upload("att-4").getStatus());
        assertEquals(409, send(senderToken, Map.of("text", "x", "tempId", "att-4")).getStatus());
        release.countDown();
        MessageDto stored = created(first.get(30, TimeUnit.SECONDS));
        assertEquals(claimed, stored.getId().toString());
        assertEquals(stored.getId(), created(upload("att-4")).getId());
        assertEquals(1, count(MESSAGES));
        storageMockServer.verify(anyUpload, VerificationTimes.exactly(1));
      } finally {
        release.countDown();
        pool.shutdownNow();
      }
    }

    @Test
    @DisplayName("The same tempId twice: one blob, one metadata row, one event, same answer")
    void sameTempIdTwice() throws Exception {
      uploadsAnswer(201, Times.unlimited());
      String queue = bindQueue(member);
      MessageDto first = created(upload("att-1"));
      assertEquals("MessageReceived", nextEvent(queue).get("type").asText());

      MessageDto second = created(upload("att-1"));

      assertEquals(first.getId(), second.getId());
      assertEquals(first.getAttachmentId(), second.getAttachmentId());
      assertEquals(
          1,
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .size());
      assertEquals(1, count(MESSAGES));
      storageMockServer.verify(anyUpload, VerificationTimes.exactly(1));
      assertNextEventIsTheSentinel(queue);
    }

    @Test
    @DisplayName("A failed upload releases its tempId so that the retry creates the message")
    void failedUploadReleasesTheTempId() throws Exception {
      uploadsAnswer(500, Times.once());
      uploadsAnswer(201, Times.unlimited());

      assertEquals(424, upload("att-2").getStatus());
      assertEquals(0, count(CLAIMS));
      assertEquals(0, count(MESSAGES));

      MessageDto retried = created(upload("att-2"));

      assertEquals(1, count(MESSAGES));
      assertEquals(retried.getId().toString(), claimOf(sender, "att-2"));
    }

    @Test
    @DisplayName("A text send with the tempId of an upload answers the upload message")
    void textRetryOfAnUploadedTempId() throws Exception {
      uploadsAnswer(201, Times.unlimited());
      MessageDto uploaded = created(upload("att-3"));

      MessageDto retried = created(send(senderToken, Map.of("text", "x", "tempId", "att-3")));

      assertEquals(uploaded.getId(), retried.getId());
      assertEquals(uploaded.getAttachmentId(), retried.getAttachmentId());
      assertEquals(1, count(MESSAGES));
    }

    @Test
    @DisplayName("An upload tempId longer than 64 characters stores nothing")
    void tempIdTooLong() throws Exception {
      uploadsAnswer(201, Times.unlimited());

      assertEquals(400, upload("t".repeat(65)).getStatus());
      storageMockServer.verify(anyUpload, VerificationTimes.never());
      assertEquals(0, count(CLAIMS));
    }
  }
}
