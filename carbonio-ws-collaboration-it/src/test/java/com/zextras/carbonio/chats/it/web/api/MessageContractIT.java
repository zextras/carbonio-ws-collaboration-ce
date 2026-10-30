// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.Parameter.param;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.storages.internal.pojo.Query;
import com.zextras.storages.internal.pojo.StoragesUploadResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.http.entity.ContentType;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;

@ApiIntegrationTest
class MessageContractIT {

  private static final String BOUNDARY = "message-contract-boundary";

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final Channel channel;
  private final StorageMockServer storageMockServer;
  private final FileMetadataRepository fileMetadataRepository;
  private final MessageRepository messageRepository;

  private static UUID aUserId;
  private static UUID bUserId;
  private static String aToken;
  private static String bToken;

  private UUID roomId;

  public MessageContractIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      Channel channel,
      StorageMockServer storageMockServer,
      FileMetadataRepository fileMetadataRepository,
      MessageRepository messageRepository) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.channel = channel;
    this.storageMockServer = storageMockServer;
    this.fileMetadataRepository = fileMetadataRepository;
    this.messageRepository = messageRepository;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @BeforeAll
  static void initAll() {
    aUserId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    aToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    bUserId = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    bToken = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
  }

  @BeforeEach
  void setUp() {
    roomId = newRoom();
    storageMockServer
        .when(
            request()
                .withMethod("PUT")
                .withPath("/upload")
                .withQueryStringParameter(param("type", "chats")))
        .respond(
            HttpResponse.response()
                .withStatusCode(201)
                .withBody(
                    JsonBody.json(
                        new StorageMockServer.UploadResponse()
                            .digest("")
                            .digestAlgorithm("")
                            .size(3L))));
  }

  private UUID newRoom() {
    UUID id = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        id, RoomTypeDto.GROUP, "message-contract", List.of(aUserId, bUserId));
    return id;
  }

  private String bindQueue(UUID user) throws Exception {
    channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
    String queue = channel.queueDeclare().getQueue();
    channel.queueBind(queue, user.toString(), "user-events");
    return queue;
  }

  private JsonNode nextEvent(String queue, String type) {
    return await()
        .pollDelay(Duration.ZERO)
        .atMost(5, SECONDS)
        .until(() -> pollEvent(queue, type), Objects::nonNull);
  }

  private JsonNode pollEvent(String queue, String type) throws Exception {
    GetResponse response;
    while ((response = channel.basicGet(queue, true)) != null) {
      JsonNode event =
          objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
      if (type.equals(event.get("type").asText())) {
        return event;
      }
    }
    return null;
  }

  private String sendText(UUID room, String text) throws Exception {
    MockHttpResponse response =
        dispatcher.post(
            String.format("/rooms/%s/messages", room),
            objectMapper.writeValueAsString(Map.of("text", text)),
            aToken);
    assertEquals(201, response.getStatus(), response.getContentAsString());
    return objectMapper.readTree(response.getContentAsString()).get("id").asText();
  }

  private MockHttpResponse uploadReplyingTo(String repliedId) throws Exception {
    return dispatcher.postMultipart(
        String.format("/rooms/%s/attachments", roomId),
        new byte[] {1, 2, 3},
        Map.of("contentLength", "3", "replyToId", repliedId),
        StringFormatUtils.encodeToUtf8("reply.png"),
        ContentType.create("image/png"),
        aToken);
  }

  private JsonNode timelineMessage(String messageId) throws Exception {
    MockHttpResponse response = dispatcher.get(String.format("/rooms/%s/timeline", roomId), bToken);
    assertEquals(200, response.getStatus(), response.getContentAsString());
    for (JsonNode item : objectMapper.readTree(response.getContentAsString()).get("items")) {
      if (item.hasNonNull("message") && messageId.equals(item.get("message").get("id").asText())) {
        return item.get("message");
      }
    }
    throw new AssertionError("message not in the timeline: " + messageId);
  }

  private static byte[] textPart(String name, String contentType, byte[] value) {
    ByteArrayOutputStream part = new ByteArrayOutputStream();
    part.writeBytes(
        ("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n")
            .getBytes(StandardCharsets.UTF_8));
    if (contentType != null) {
      part.writeBytes(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
    part.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
    part.writeBytes(value);
    part.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
    return part.toByteArray();
  }

  private MockHttpResponse uploadRaw(String fileName, byte[]... textParts) throws Exception {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    body.writeBytes(textPart("contentLength", null, "3".getBytes(StandardCharsets.UTF_8)));
    for (byte[] part : textParts) {
      body.writeBytes(part);
    }
    body.writeBytes(
        ("--"
                + BOUNDARY
                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + fileName
                + "\"\r\nContent-Type: image/png\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8));
    body.writeBytes(new byte[] {1, 2, 3});
    body.writeBytes(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
    return dispatcher.postRaw(
        String.format("/rooms/%s/attachments", roomId),
        body.toByteArray(),
        "multipart/form-data; boundary=" + BOUNDARY,
        aToken);
  }

  private String storedText(MockHttpResponse response) throws Exception {
    String messageId = objectMapper.readTree(response.getContentAsString()).get("id").asText();
    return integrationTestUtils.getMessageById(messageId).orElseThrow().getText();
  }

  private String storedFileName(MockHttpResponse response) throws Exception {
    String fileId =
        objectMapper.readTree(response.getContentAsString()).get("attachmentId").asText();
    return integrationTestUtils
        .getFileMetadataById(UUID.fromString(fileId))
        .orElseThrow()
        .getName();
  }

  private int attachmentCount() {
    return integrationTestUtils
        .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
        .size();
  }

  private int messageCount() {
    return messageRepository.getByRoomId(roomId.toString(), null, null, 1000).size();
  }

  @Nested
  @DisplayName("Upload MessageReceived embeds the replied message like the timeline")
  class UploadReply {

    private JsonNode repliedMessageInUploadEvent(String repliedId) throws Exception {
      String queue = bindQueue(bUserId);

      MockHttpResponse response = uploadReplyingTo(repliedId);

      assertEquals(201, response.getStatus(), response.getContentAsString());
      JsonNode message = nextEvent(queue, "MessageReceived").get("message");
      assertEquals(repliedId, message.get("replyToId").asText());
      assertEquals(
          timelineMessage(message.get("id").asText()).get("repliedMessage"),
          message.get("repliedMessage"));
      return message.get("repliedMessage");
    }

    @Test
    @DisplayName("Replying to an edited message carries editedInfo")
    void replyToEdited() throws Exception {
      String edited = sendText(roomId, "before edit");
      assertEquals(
          200,
          dispatcher
              .put(
                  String.format("/rooms/%s/messages/%s/edit", roomId, edited),
                  objectMapper.writeValueAsString(Map.of("text", "after edit")),
                  aToken)
              .getStatus());

      JsonNode replied = repliedMessageInUploadEvent(edited);

      assertEquals("after edit", replied.get("text").asText());
      assertTrue(replied.hasNonNull("editedInfo"), replied.toString());
    }

    @Test
    @DisplayName("Replying to a deleted message carries deletedInfo")
    void replyToDeleted() throws Exception {
      String deleted = sendText(roomId, "to be deleted");
      assertEquals(
          204,
          dispatcher
              .delete(String.format("/rooms/%s/messages/%s", roomId, deleted), aToken)
              .getStatus());

      JsonNode replied = repliedMessageInUploadEvent(deleted);

      assertEquals(aUserId.toString(), replied.get("deletedInfo").get("deletedBy").asText());
    }

    @Test
    @DisplayName("Replying to a forwarded message carries forwardedInfo")
    void replyToForwarded() throws Exception {
      UUID otherRoom = newRoom();
      String original = sendText(otherRoom, "forward me");
      MockHttpResponse forwardResponse =
          dispatcher.post(
              String.format("/rooms/%s/messages/%s/forward", otherRoom, original),
              objectMapper.writeValueAsString(Map.of("toRoomId", roomId.toString())),
              aToken);
      assertEquals(201, forwardResponse.getStatus(), forwardResponse.getContentAsString());
      String forwarded =
          objectMapper.readTree(forwardResponse.getContentAsString()).get("id").asText();

      JsonNode replied = repliedMessageInUploadEvent(forwarded);

      assertEquals(
          aUserId.toString(), replied.get("forwardedInfo").get("originalSenderId").asText());
    }
  }

  @Nested
  @DisplayName("Attachment file names")
  class FileNames {

    @Test
    @DisplayName("A plain ASCII name is stored as-is")
    void plainName() throws Exception {
      MockHttpResponse response = uploadRaw("foto.jpg");

      assertEquals(201, response.getStatus(), response.getContentAsString());
      assertEquals("foto.jpg", storedFileName(response));
    }

    @Test
    @DisplayName("A raw UTF-8 name is stored as-is")
    void rawUtf8Name() throws Exception {
      MockHttpResponse response = uploadRaw("foto àè.jpg");

      assertEquals(201, response.getStatus(), response.getContentAsString());
      assertEquals("foto àè.jpg", storedFileName(response));
    }

    @Test
    @DisplayName("A fully escaped name, as the official clients send it, is decoded")
    void escapedName() throws Exception {
      MockHttpResponse response =
          uploadRaw("\\ud83d\\ude00" + StringFormatUtils.encodeToUtf8(" àè.jpg"));

      assertEquals(201, response.getStatus(), response.getContentAsString());
      assertEquals("😀 àè.jpg", storedFileName(response));
    }

    @Test
    @DisplayName("A name decoding to a lone surrogate is a 400 and stores nothing")
    void loneSurrogate() throws Exception {
      MockHttpResponse response = uploadRaw("\\ud83d");

      assertEquals(400, response.getStatus(), response.getContentAsString());
      assertEquals(0, attachmentCount());
      assertEquals(0, messageCount());
    }
  }

  @Nested
  @DisplayName("Multipart text parts")
  class MultipartText {

    @Test
    @DisplayName("A UTF-8 description without a part content type is stored intact")
    void utf8DescriptionWithoutContentType() throws Exception {
      String description = "— \"àèì\" 👍";

      MockHttpResponse response =
          uploadRaw(
              StringFormatUtils.encodeToUtf8("photo.png"),
              textPart("description", null, description.getBytes(StandardCharsets.UTF_8)));

      assertEquals(201, response.getStatus(), response.getContentAsString());
      assertEquals(description, storedText(response));
    }

    @Test
    @DisplayName("An explicit ISO-8859-1 part charset is still honoured")
    void isoDescriptionWithExplicitCharset() throws Exception {
      MockHttpResponse response =
          uploadRaw(
              StringFormatUtils.encodeToUtf8("photo.png"),
              textPart(
                  "description",
                  "text/plain; charset=ISO-8859-1",
                  "àèì".getBytes(StandardCharsets.ISO_8859_1)));

      assertEquals(201, response.getStatus(), response.getContentAsString());
      assertEquals("àèì", storedText(response));
    }
  }

  @Nested
  @DisplayName("Forwarded attachments")
  class ForwardArea {

    @Test
    @DisplayName("A forwarded image keeps its area in the database, the event and the timeline")
    void forwardKeepsArea() throws Exception {
      UUID sourceRoom = newRoom();
      String fileId = UUID.randomUUID().toString();
      String messageId = UUID.randomUUID().toString();
      fileMetadataRepository.save(
          FileMetadata.create()
              .id(fileId)
              .name("pic.png")
              .originalSize(3L)
              .mimeType("image/png")
              .type(FileMetadataType.ATTACHMENT)
              .userId(aUserId.toString())
              .roomId(sourceRoom.toString())
              .messageId(messageId)
              .area("640x480"));
      messageRepository.insert(
          Message.create()
              .id(messageId)
              .roomId(sourceRoom.toString())
              .senderId(aUserId.toString())
              .text("")
              .attachmentId(fileId));
      StoragesUploadResponse copied = new StoragesUploadResponse();
      copied.setQuery(new Query());
      copied.getQuery().setType("chats");
      storageMockServer
          .when(
              request()
                  .withMethod("PUT")
                  .withPath("/copy")
                  .withQueryStringParameter(param("sourceNode", fileId)))
          .respond(HttpResponse.response().withStatusCode(200).withBody(JsonBody.json(copied)));
      String queue = bindQueue(bUserId);

      MockHttpResponse response =
          dispatcher.post(
              String.format("/rooms/%s/messages/%s/forward", sourceRoom, messageId),
              objectMapper.writeValueAsString(Map.of("toRoomId", roomId.toString())),
              aToken);

      assertEquals(201, response.getStatus(), response.getContentAsString());
      JsonNode forwarded = objectMapper.readTree(response.getContentAsString());
      String copyId = forwarded.get("attachmentId").asText();
      assertEquals(
          "640x480",
          integrationTestUtils
              .getFileMetadataById(UUID.fromString(copyId))
              .orElseThrow()
              .getArea());
      JsonNode event = nextEvent(queue, "MessageForwarded");
      assertEquals("640x480", event.get("message").get("attachment").path("area").asText());
      assertEquals(
          "640x480",
          timelineMessage(forwarded.get("id").asText()).get("attachment").path("area").asText());
    }
  }

  @Nested
  @DisplayName("Message and page limits")
  class Limits {

    private MockHttpResponse send(Map<String, String> body) throws Exception {
      return dispatcher.post(
          String.format("/rooms/%s/messages", roomId),
          objectMapper.writeValueAsString(body),
          aToken);
    }

    @Test
    @DisplayName("A blank text is a 400 and stores nothing")
    void blankText() throws Exception {
      assertEquals(400, send(Map.of("text", " \n\t ")).getStatus());
      assertEquals(400, send(Map.of("text", "")).getStatus());
      assertEquals(0, messageCount());
    }

    @Test
    @DisplayName("A text of 8192 characters is accepted, 8193 is a 400")
    void textLength() throws Exception {
      assertEquals(201, send(Map.of("text", "x".repeat(8192))).getStatus());
      assertEquals(400, send(Map.of("text", "x".repeat(8193))).getStatus());
      assertEquals(1, messageCount());
    }

    @Test
    @DisplayName("A tempId of 64 characters is accepted, 65 is a 400")
    void tempIdLength() throws Exception {
      assertEquals(201, send(Map.of("text", "hi", "tempId", "t".repeat(64))).getStatus());
      assertEquals(400, send(Map.of("text", "hi", "tempId", "t".repeat(65))).getStatus());
    }

    @Test
    @DisplayName("An edit longer than 8192 characters is a 400")
    void editLength() throws Exception {
      String messageId = sendText(roomId, "short");
      String path = String.format("/rooms/%s/messages/%s/edit", roomId, messageId);

      assertEquals(
          400,
          dispatcher
              .put(path, objectMapper.writeValueAsString(Map.of("text", "x".repeat(8193))), aToken)
              .getStatus());
      assertEquals(
          200,
          dispatcher
              .put(path, objectMapper.writeValueAsString(Map.of("text", "x".repeat(8192))), aToken)
              .getStatus());
    }

    @Test
    @DisplayName("Upload description and tempId over their limits are a 400 and store nothing")
    void uploadLimits() throws Exception {
      String fileName = StringFormatUtils.encodeToUtf8("photo.png");

      MockHttpResponse longDescription =
          uploadRaw(
              fileName,
              textPart("description", null, "d".repeat(8193).getBytes(StandardCharsets.UTF_8)));
      MockHttpResponse longTempId =
          uploadRaw(
              fileName, textPart("tempId", null, "t".repeat(65).getBytes(StandardCharsets.UTF_8)));

      assertEquals(400, longDescription.getStatus());
      assertEquals(400, longTempId.getStatus());
      assertEquals(0, attachmentCount());
      assertEquals(0, messageCount());
    }

    @Test
    @DisplayName("Timeline and search accept a limit between 1 and 200")
    void pageLimits() throws Exception {
      for (String path :
          List.of(
              String.format("/rooms/%s/timeline?limit=", roomId),
              String.format("/rooms/%s/messages/search?q=a&limit=", roomId))) {
        assertEquals(400, dispatcher.get(path + "0", aToken).getStatus(), path);
        assertEquals(400, dispatcher.get(path + "201", aToken).getStatus(), path);
        assertEquals(200, dispatcher.get(path + "1", aToken).getStatus(), path);
        assertEquals(200, dispatcher.get(path + "200", aToken).getStatus(), path);
      }
    }
  }

  @Nested
  @DisplayName("Removed endpoints")
  class Removed {

    @Test
    @DisplayName("GET /rooms/{roomId}/messages no longer exists, POST on the same path still works")
    void messageHistoryIsGone() throws Exception {
      assertEquals(
          405, dispatcher.get(String.format("/rooms/%s/messages", roomId), aToken).getStatus());
      sendText(roomId, "still works");
    }
  }
}
