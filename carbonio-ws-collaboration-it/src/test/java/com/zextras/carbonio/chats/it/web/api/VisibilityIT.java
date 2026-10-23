// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.AttachmentsApi;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.http.entity.ContentType;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ApiIntegrationTest
class VisibilityIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final MessagePinRepository pinRepository;
  private final ReadMarkerRepository readRepository;
  private final Channel channel;
  private final StorageMockServer storageMockServer;
  private final FileMetadataRepository fileMetadataRepository;
  private final AppClock clock;

  private static UUID aUserId;
  private static UUID bUserId;
  private static UUID cUserId;
  private static String aToken;
  private static String bToken;
  private static String cToken;

  private static final OffsetDateTime T0 =
      OffsetDateTime.of(2026, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
  private static final OffsetDateTime T1 = T0.plusHours(2);
  private static final OffsetDateTime T1_EVENT = T1.plusSeconds(1);
  private static final OffsetDateTime T2 = T1.plusSeconds(10);
  private static final OffsetDateTime T2_AFTER = T2.plusNanos(1_000_000);
  private static final OffsetDateTime T3 = T2.plusSeconds(10);
  private static final OffsetDateTime T4 = T3.plusSeconds(10);
  private static final OffsetDateTime T3_5 = T3.plusSeconds(5);
  private static final OffsetDateTime T5 = T4.plusSeconds(10);

  private UUID roomId;
  private String m1;
  private String m2;
  private String m3;
  private String fileOld;
  private String fileNew;
  private String eventOld;
  private String eventAtCleared;
  private String eventAfterCleared;

  public VisibilityIT(
      RoomsApi roomsApi,
      AttachmentsApi attachmentsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      MessageRepository messageRepository,
      SystemEventRepository systemEventRepository,
      SubscriptionRepository subscriptionRepository,
      MessagePinRepository pinRepository,
      ReadMarkerRepository readRepository,
      Channel channel,
      StorageMockServer storageMockServer,
      FileMetadataRepository fileMetadataRepository,
      Clock clock) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.messageRepository = messageRepository;
    this.systemEventRepository = systemEventRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.pinRepository = pinRepository;
    this.readRepository = readRepository;
    this.channel = channel;
    this.storageMockServer = storageMockServer;
    this.fileMetadataRepository = fileMetadataRepository;
    this.clock = (AppClock) clock;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
    this.dispatcher.getRegistry().addSingletonResource(attachmentsApi);
  }

  @BeforeAll
  static void initAll() {
    aUserId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    aToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    bUserId = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    bToken = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
    cUserId = MockedAccount.getAccount(MockedAccountType.LINUS_VAN_PELT).getUUID();
    cToken = MockedAccount.getAccount(MockedAccountType.LINUS_VAN_PELT).getToken();
  }

  @BeforeEach
  void setUp() {
    roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "visibility-room", List.of(aUserId, bUserId, cUserId));
    clearHistoryOf(bUserId, T2);

    m1 = saveMessage(roomId, aUserId, "needle-one", T1, null);
    m2 = saveMessage(roomId, aUserId, "second", T3, m1);
    m3 = saveMessage(roomId, aUserId, "third", T4, null);
    eventOld = UUID.randomUUID().toString();
    saveEvent(eventOld, "SOMETHING_OLD", T1_EVENT);
    eventAtCleared = UUID.randomUUID().toString();
    saveEvent(eventAtCleared, "SOMETHING_AT_CLEAR", T2);
    eventAfterCleared = UUID.randomUUID().toString();
    saveEvent(eventAfterCleared, "SOMETHING_AFTER_CLEAR", T2_AFTER);
    fileOld = saveFile("old.png", T1);
    fileNew = saveFile("new.png", T3);
    pinRepository.pin(roomId.toString(), m1, aUserId.toString());
  }

  private String saveMessage(
      UUID room, UUID sender, String text, OffsetDateTime at, String replyToId) {
    String id = UUID.randomUUID().toString();
    clock.fixTimeAt(at.toInstant());
    messageRepository.insert(
        Message.create()
            .id(id)
            .roomId(room.toString())
            .senderId(sender.toString())
            .text(text)
            .replyToId(replyToId));
    clock.removeFixTime();
    return id;
  }

  private void saveEvent(String id, String type, OffsetDateTime at) {
    systemEventRepository.insert(
        SystemEvent.create()
            .id(id)
            .roomId(roomId.toString())
            .type(type)
            .actorId(aUserId.toString())
            .content("{}")
            .createdAt(at));
  }

  private String saveFile(String name, OffsetDateTime at) {
    UUID id = UUID.randomUUID();
    clock.fixTimeAt(at.toInstant());
    integrationTestUtils.generateAndSaveFileMetadata(
        id, name, "image/png", FileMetadataType.ATTACHMENT, aUserId, roomId);
    clock.removeFixTime();
    return id.toString();
  }

  private void clearHistoryOf(UUID user, OffsetDateTime at) {
    Room room = integrationTestUtils.getRoomById(roomId).orElseThrow();
    integrationTestUtils.setRoomUserSettings(
        RoomUserSettings.create(room, user.toString()).clearedAt(at));
  }

  private JsonNode json(MockHttpResponse response) throws Exception {
    assertEquals(200, response.getStatus());
    return objectMapper.readTree(response.getContentAsString());
  }

  private List<JsonNode> timelineItems(String query, String token) throws Exception {
    JsonNode root =
        json(dispatcher.get(String.format("/rooms/%s/timeline%s", roomId, query), token));
    List<JsonNode> items = new ArrayList<>();
    root.get("items").forEach(items::add);
    return items;
  }

  private static String itemId(JsonNode item) {
    return item.has("message") && !item.get("message").isNull()
        ? item.get("message").get("id").asText()
        : item.get("systemEvent").get("id").asText();
  }

  private static List<String> ids(List<JsonNode> items) {
    return items.stream().map(VisibilityIT::itemId).toList();
  }

  private static OffsetDateTime createdAt(JsonNode item) {
    return OffsetDateTime.parse(item.get("createdAt").asText());
  }

  private static String enc(OffsetDateTime t) {
    return URLEncoder.encode(t.toString(), StandardCharsets.UTF_8);
  }

  private JsonNode messageItem(List<JsonNode> items, String id) {
    return items.stream()
        .filter(i -> id.equals(itemId(i)))
        .findFirst()
        .orElseThrow(() -> new AssertionError("item not in page: " + id))
        .get("message");
  }

  private static boolean hasQuote(JsonNode message) {
    return message.has("repliedMessage") && !message.get("repliedMessage").isNull();
  }

  private int messageCount() {
    return messageRepository.getByRoomId(roomId.toString(), null, null, 1000).size();
  }

  private static void assertNothingUpTo(List<JsonNode> items, OffsetDateTime bound) {
    items.forEach(i -> assertTrue(createdAt(i).isAfter(bound), "hidden item leaked: " + itemId(i)));
  }

  private MockHttpResponse putJson(String path, Object body, String token) throws Exception {
    return dispatcher.put(path, objectMapper.writeValueAsString(body), token);
  }

  private MockHttpResponse markRead(String messageId, String token) throws Exception {
    return putJson(String.format("/rooms/%s/read", roomId), Map.of("itemId", messageId), token);
  }

  private MockHttpResponse react(String messageId, String token) throws Exception {
    String emoji = URLEncoder.encode("\uD83D\uDC4D", StandardCharsets.UTF_8);
    return dispatcher.post(
        String.format("/rooms/%s/messages/%s/reactions/%s", roomId, messageId, emoji), token);
  }

  private MockHttpResponse pin(String messageId, String token) throws Exception {
    return dispatcher.put(
        String.format("/rooms/%s/messages/%s/pin", roomId, messageId), null, token);
  }

  private MockHttpResponse reply(String token, String replyTo) throws Exception {
    return dispatcher.post(
        String.format("/rooms/%s/messages", roomId),
        objectMapper.writeValueAsString(Map.of("text", "re", "replyToId", replyTo)),
        token);
  }

  private MockHttpResponse forward(String token, String messageId, UUID target) throws Exception {
    return dispatcher.post(
        String.format("/rooms/%s/messages/%s/forward", roomId, messageId),
        objectMapper.writeValueAsString(Map.of("toRoomId", target.toString())),
        token);
  }

  private UUID otherRoom() {
    UUID other = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        other, RoomTypeDto.GROUP, "other-room", List.of(aUserId, bUserId, cUserId));
    return other;
  }

  @Nested
  @DisplayName("Timeline floor (cleared_at)")
  class TimelineFloor {

    @Test
    @DisplayName("Latest page hides everything at or before cleared_at")
    void latest() throws Exception {
      List<JsonNode> items = timelineItems("", bToken);
      assertNothingUpTo(items, T2);
      assertTrue(ids(items).containsAll(List.of(eventAfterCleared, m2, m3)));
      assertFalse(ids(items).contains(eventAtCleared));
      assertFalse(ids(items).contains(m1));
    }

    @Test
    @DisplayName("Before-page hides everything at or before cleared_at")
    void before() throws Exception {
      assertNothingUpTo(timelineItems("?before=" + enc(T5), bToken), T2);
    }

    @Test
    @DisplayName("After-page hides everything at or before cleared_at")
    void after() throws Exception {
      List<JsonNode> items = timelineItems("?after=" + enc(T0), bToken);
      assertNothingUpTo(items, T2);
      assertTrue(ids(items).contains(eventAfterCleared));
    }

    @Test
    @DisplayName("Around-page anchored before cleared_at hides everything at or before it")
    void around() throws Exception {
      List<JsonNode> items = timelineItems("?around=" + enc(T1), bToken);
      assertNothingUpTo(items, T2);
      assertTrue(ids(items).contains(eventAfterCleared));
    }

    @Test
    @DisplayName("Around with an explicit after honours the later of after and cleared_at")
    void aroundWithExplicitAfter() throws Exception {
      List<JsonNode> forB = timelineItems("?around=" + enc(T1) + "&after=" + enc(T3), bToken);
      assertFalse(forB.isEmpty());
      assertNothingUpTo(forB, T3);
      assertTrue(ids(forB).contains(m3));

      List<JsonNode> forC = timelineItems("?around=" + enc(T1) + "&after=" + enc(T1_EVENT), cToken);
      assertFalse(forC.isEmpty());
      assertNothingUpTo(forC, T1_EVENT);
    }

    @Test
    @DisplayName("An item exactly at cleared_at is hidden, one just after is visible")
    void boundary() throws Exception {
      String atClear = saveMessage(roomId, aUserId, "at-clear", T2, null);
      String afterClear = saveMessage(roomId, aUserId, "after-clear", T2_AFTER, null);
      for (String query :
          List.of("", "?before=" + enc(T5), "?after=" + enc(T0), "?around=" + enc(T2))) {
        List<String> ids = ids(timelineItems(query, bToken));
        assertFalse(ids.contains(atClear), query);
        assertTrue(ids.contains(afterClear), query);
      }
    }

    @Test
    @DisplayName("Members without cleared_at see everything since room creation")
    void noClearedAtSeesAll() throws Exception {
      for (String token : List.of(aToken, cToken)) {
        List<JsonNode> items = timelineItems("", token);
        assertTrue(
            ids(items)
                .containsAll(List.of(m1, m2, m3, eventOld, eventAtCleared, eventAfterCleared)));
      }
    }
  }

  @Nested
  @DisplayName("Quoted originals")
  class Quotes {

    @Test
    @DisplayName("A reply to an original hidden by cleared_at has no repliedMessage")
    void hiddenOriginalNotQuoted() throws Exception {
      JsonNode forB = messageItem(timelineItems("", bToken), m2);
      assertFalse(forB.has("replyToId"));
      assertFalse(hasQuote(forB));

      for (String token : List.of(aToken, cToken)) {
        JsonNode quoted = messageItem(timelineItems("", token), m2);
        assertEquals("needle-one", quoted.get("repliedMessage").get("text").asText());
      }
    }

    @Test
    @DisplayName("After a manual clear, A loses m1 and its quotes but keeps newer quotes")
    void manualClearHidesQuotes() throws Exception {
      clearHistoryOf(aUserId, T3_5);
      String replyToOld = saveMessage(roomId, aUserId, "fourth", T5, m1);
      String replyToNew = saveMessage(roomId, aUserId, "fifth", T5.plusSeconds(1), m3);

      List<JsonNode> itemsA = timelineItems("", aToken);
      assertNothingUpTo(itemsA, T3_5);
      assertFalse(ids(itemsA).contains(m1));
      assertFalse(ids(itemsA).contains(m2));
      assertFalse(hasQuote(messageItem(itemsA, replyToOld)));
      assertEquals(
          "third", messageItem(itemsA, replyToNew).get("repliedMessage").get("text").asText());
      assertNothingUpTo(timelineItems("?around=" + enc(T5), aToken), T3_5);
    }
  }

  @Nested
  @DisplayName("Search, pins, history, attachments, export")
  class Reads {

    @Test
    @DisplayName("Search for a hidden text returns nothing for B, a hit for A and C")
    void search() throws Exception {
      String path = String.format("/rooms/%s/messages/search?q=needle", roomId);
      assertEquals(0, json(dispatcher.get(path, bToken)).get("messages").size());
      assertEquals(1, json(dispatcher.get(path, aToken)).get("messages").size());
      assertEquals(1, json(dispatcher.get(path, cToken)).get("messages").size());
    }

    @Test
    @DisplayName("Every pin is returned to everyone with the full pinned message")
    void pins() throws Exception {
      for (String token : List.of(aToken, bToken, cToken)) {
        JsonNode pins = json(dispatcher.get(String.format("/rooms/%s/pin", roomId), token));
        assertEquals(1, pins.size());
        assertEquals(m1, pins.get(0).get("messageId").asText());
        assertEquals(m1, pins.get(0).get("message").get("id").asText());
        assertEquals("needle-one", pins.get(0).get("message").get("text").asText());
        assertEquals(
            T1.toInstant(), createdAt(pins.get(0).get("message"), "createdAt").toInstant());
        assertFalse(pins.get(0).has("text"));
        assertFalse(pins.get(0).has("messageCreatedAt"));
      }
    }

    private OffsetDateTime createdAt(JsonNode node, String field) {
      return OffsetDateTime.parse(node.get(field).asText());
    }

    @Test
    @DisplayName("History and search hide the reply reference when the original is hidden")
    void replyReferenceInHistoryAndSearch() throws Exception {
      for (String token : List.of(aToken, bToken)) {
        boolean visible = token.equals(aToken);
        JsonNode history = json(dispatcher.get(String.format("/rooms/%s/messages", roomId), token));
        JsonNode search =
            json(dispatcher.get(String.format("/rooms/%s/messages/search?q=second", roomId), token))
                .get("messages");
        for (JsonNode list : List.of(history, search)) {
          JsonNode reply = null;
          for (JsonNode n : list) {
            if (m2.equals(n.get("id").asText())) {
              reply = n;
            }
          }
          assertNotNull(reply);
          assertEquals(visible, reply.has("replyToId"));
        }
      }
    }

    @Test
    @DisplayName("Message history excludes hidden messages for B")
    void history() throws Exception {
      List<String> idsB = new ArrayList<>();
      json(dispatcher.get(String.format("/rooms/%s/messages", roomId), bToken))
          .forEach(n -> idsB.add(n.get("id").asText()));
      assertFalse(idsB.contains(m1));
      assertTrue(idsB.contains(m2));

      List<String> idsUpTo = new ArrayList<>();
      json(dispatcher.get(String.format("/rooms/%s/messages?before=%s", roomId, m3), bToken))
          .forEach(n -> idsUpTo.add(n.get("id").asText()));
      assertFalse(idsUpTo.contains(m1));
      assertTrue(idsUpTo.contains(m2));

      List<String> idsC = new ArrayList<>();
      json(dispatcher.get(String.format("/rooms/%s/messages", roomId), cToken))
          .forEach(n -> idsC.add(n.get("id").asText()));
      assertTrue(idsC.contains(m1));
    }

    @Test
    @DisplayName("Attachment list excludes hidden files for B")
    void attachments() throws Exception {
      List<String> idsB = new ArrayList<>();
      JsonNode pageB = json(dispatcher.get(String.format("/rooms/%s/attachments", roomId), bToken));
      pageB.get("attachments").forEach(n -> idsB.add(n.get("id").asText()));
      assertEquals(List.of(fileNew), idsB);
      assertEquals(1, pageB.get("total").asInt());

      JsonNode pageC = json(dispatcher.get(String.format("/rooms/%s/attachments", roomId), cToken));
      assertEquals(2, pageC.get("attachments").size());
    }

    private String attachmentOf(String name, OffsetDateTime fileAt, OffsetDateTime messageAt) {
      String fileId = saveFile(name, fileAt);
      String messageId = UUID.randomUUID().toString();
      clock.fixTimeAt(messageAt.toInstant());
      messageRepository.insert(
          Message.create()
              .id(messageId)
              .roomId(roomId.toString())
              .senderId(aUserId.toString())
              .text(name)
              .attachmentId(fileId));
      clock.removeFixTime();
      fileMetadataRepository.save(
          integrationTestUtils
              .getFileMetadataById(UUID.fromString(fileId))
              .orElseThrow()
              .messageId(messageId));
      return fileId;
    }

    @Test
    @DisplayName("List and by-id access follow the owning message time, not the file time")
    void attachmentsFollowTheMessageTime() throws Exception {
      String visibleByMessage = attachmentOf("late-message.png", T1, T4);
      String hiddenByMessage = attachmentOf("early-message.png", T4, T1);

      List<String> idsB = new ArrayList<>();
      json(dispatcher.get(String.format("/rooms/%s/attachments", roomId), bToken))
          .get("attachments")
          .forEach(n -> idsB.add(n.get("id").asText()));
      assertTrue(idsB.contains(visibleByMessage));
      assertFalse(idsB.contains(hiddenByMessage));

      assertEquals(
          200,
          dispatcher.get(String.format("/attachments/%s", visibleByMessage), bToken).getStatus());
      assertEquals(
          404,
          dispatcher.get(String.format("/attachments/%s", hiddenByMessage), bToken).getStatus());
    }

    @Test
    @DisplayName("Export excludes hidden messages for B")
    void export() throws Exception {
      MockHttpResponse forB =
          dispatcher.get(String.format("/rooms/%s/messages/export", roomId), bToken);
      assertEquals(200, forB.getStatus());
      assertFalse(forB.getContentAsString().contains("needle-one"));
      assertTrue(forB.getContentAsString().contains("second"));

      MockHttpResponse forC =
          dispatcher.get(String.format("/rooms/%s/messages/export", roomId), cToken);
      assertTrue(forC.getContentAsString().contains("needle-one"));
    }

    @Test
    @DisplayName("Attachment by id, info and download are 404 for B when hidden, fine for C")
    void attachmentById() throws Exception {
      assertEquals(
          404, dispatcher.get(String.format("/attachments/%s", fileOld), bToken).getStatus());
      assertEquals(
          404,
          dispatcher.get(String.format("/attachments/%s/download", fileOld), bToken).getStatus());
      assertEquals(
          200, dispatcher.get(String.format("/attachments/%s", fileNew), bToken).getStatus());
      assertEquals(
          200, dispatcher.get(String.format("/attachments/%s", fileOld), cToken).getStatus());
    }
  }

  @Nested
  @DisplayName("Actions on hidden items")
  class Actions {

    @Test
    @DisplayName("B replying to a hidden message gets 404 and nothing is inserted")
    void replyToHidden() throws Exception {
      int before = messageCount();
      assertEquals(404, reply(bToken, m1).getStatus());
      assertEquals(before, messageCount());
    }

    @Test
    @DisplayName("Reply to an item exactly at cleared_at is 404, just after it is 201")
    void replyBoundary() throws Exception {
      String atClear = saveMessage(roomId, aUserId, "at-clear", T2, null);
      String afterClear = saveMessage(roomId, aUserId, "after-clear", T2_AFTER, null);
      assertEquals(404, reply(bToken, atClear).getStatus());
      assertEquals(201, reply(bToken, afterClear).getStatus());
      assertEquals(201, reply(bToken, m2).getStatus());
    }

    @Test
    @DisplayName("Replying to a message of another room gets 404 and nothing is inserted")
    void replyToOtherRoom() throws Exception {
      UUID other = otherRoom();
      String foreign = saveMessage(other, aUserId, "foreign", T3, null);
      int before = messageCount();
      assertEquals(404, reply(aToken, foreign).getStatus());
      assertEquals(404, reply(cToken, foreign).getStatus());
      assertEquals(before, messageCount());
    }

    @Test
    @DisplayName("A non-member still gets 403 when sending a reply")
    void nonMemberReply() throws Exception {
      String outsiderToken = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getToken();
      assertEquals(403, reply(outsiderToken, m1).getStatus());
    }

    @Test
    @DisplayName("B forward, pin, react and read of a hidden message are all 404")
    void hiddenActions() throws Exception {
      UUID other = otherRoom();
      assertEquals(404, forward(bToken, m1, other).getStatus());
      assertEquals(0, messageRepository.getByRoomId(other.toString(), null, null, 10).size());
      assertEquals(404, pin(m1, bToken).getStatus());
      assertEquals(404, react(m1, bToken).getStatus());
      for (String id : List.of(m1, eventOld, eventAtCleared)) {
        assertEquals(404, markRead(id, bToken).getStatus(), id);
      }
      assertTrue(readRepository.get(bUserId.toString(), roomId.toString()).isEmpty());
    }

    @Test
    @DisplayName("An attachment replying to a hidden target is 404 and stores nothing")
    void attachmentReplyToHidden() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      int messagesBefore = messageCount();
      int filesBefore =
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .size();

      MockHttpResponse response =
          dispatcher.postMultipart(
              String.format("/rooms/%s/attachments", roomId),
              fileMock.getFileBytes(),
              Map.of(
                  "mimeType",
                  fileMock.getMimeType(),
                  "contentLength",
                  String.valueOf(fileMock.getSize()),
                  "replyToId",
                  m1),
              StringFormatUtils.encodeToUtf8(fileMock.getName()),
              ContentType.parse(fileMock.getMimeType()),
              bToken);

      assertEquals(404, response.getStatus(), response.getContentAsString());
      assertEquals(messagesBefore, messageCount());
      assertEquals(
          filesBefore,
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .size());
    }

    @Test
    @DisplayName("B can mark read an item just after cleared_at")
    void readAfterCleared() throws Exception {
      assertEquals(204, markRead(eventAfterCleared, bToken).getStatus());
    }

    @Test
    @DisplayName("C and A, with no cleared_at, can reply, forward, react, read and pin m1")
    void unclearedMembersCanActOnOldMessages() throws Exception {
      for (String token : List.of(cToken, aToken)) {
        assertEquals(201, reply(token, m1).getStatus());
        assertEquals(201, forward(token, m1, otherRoom()).getStatus());
        assertEquals(204, react(m1, token).getStatus());
        assertEquals(204, markRead(m1, token).getStatus());
        assertEquals(204, pin(m1, token).getStatus());
      }
    }

    @Test
    @DisplayName("After a manual clear A can no longer act on messages up to the clear")
    void manualClearBlocksActions() throws Exception {
      clearHistoryOf(aUserId, T3_5);
      assertEquals(404, reply(aToken, m1).getStatus());
      assertEquals(404, pin(m1, aToken).getStatus());
      assertEquals(201, reply(aToken, m3).getStatus());
    }

    @Test
    @DisplayName("B can unpin a pinned message below its cleared_at")
    void unpinHidden() throws Exception {
      assertEquals(
          204,
          dispatcher
              .delete(String.format("/rooms/%s/messages/%s/pin", roomId, m1), bToken)
              .getStatus());
    }
  }

  @Nested
  @DisplayName("WebSocket events")
  class WsEvents {

    private String bindQueue(UUID user) throws Exception {
      channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
      String queue = channel.queueDeclare().getQueue();
      channel.queueBind(queue, user.toString(), "user-events");
      return queue;
    }

    private JsonNode nextEvent(String queue) throws Exception {
      for (int i = 0; i < 50; i++) {
        GetResponse response = channel.basicGet(queue, true);
        if (response != null) {
          return objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
        }
        Thread.sleep(100);
      }
      throw new AssertionError("no WS event received on " + queue);
    }

    @Test
    @DisplayName("MessageReceived embeds the quoted original only for members who can see it")
    void quoteSplit() throws Exception {
      String queueA = bindQueue(aUserId);
      String queueB = bindQueue(bUserId);
      String queueC = bindQueue(cUserId);

      assertEquals(201, reply(aToken, m1).getStatus());

      for (String queue : List.of(queueA, queueC)) {
        JsonNode event = nextEvent(queue);
        assertEquals("MessageReceived", event.get("type").asText());
        assertEquals("needle-one", event.get("message").get("repliedMessage").get("text").asText());
      }
      JsonNode forB = nextEvent(queueB);
      assertEquals("MessageReceived", forB.get("type").asText());
      assertFalse(forB.get("message").has("replyToId"));
      assertFalse(hasQuote(forB.get("message")));
      assertFalse(forB.toString().contains("needle-one"));
    }

    @Test
    @DisplayName("MessagePinned carries the same message as GET pin for every member")
    void pinnedPlainMessage() throws Exception {
      String queueA = bindQueue(aUserId);
      String queueB = bindQueue(bUserId);
      String queueC = bindQueue(cUserId);

      assertEquals(204, pin(m3, aToken).getStatus());

      JsonNode expected =
          json(dispatcher.get(String.format("/rooms/%s/pin", roomId), aToken))
              .get(0)
              .get("message");
      assertEquals("third", expected.get("text").asText());
      for (String queue : List.of(queueA, queueB, queueC)) {
        JsonNode event = nextEvent(queue);
        assertEquals("MessagePinned", event.get("type").asText());
        assertEquals(m3, event.get("messageId").asText());
        assertEquals(expected, event.get("message"));
      }
    }

    @Test
    @DisplayName("MessagePinned of a reply embeds the original only for members who can see it")
    void pinnedReplySplit() throws Exception {
      String queueA = bindQueue(aUserId);
      String queueB = bindQueue(bUserId);
      String queueC = bindQueue(cUserId);

      assertEquals(204, pin(m2, aToken).getStatus());

      for (String queue : List.of(queueA, queueC)) {
        JsonNode event = nextEvent(queue);
        assertEquals("MessagePinned", event.get("type").asText());
        assertEquals(m1, event.get("message").get("replyToId").asText());
        assertEquals("needle-one", event.get("message").get("repliedMessage").get("text").asText());
      }
      JsonNode forB = nextEvent(queueB);
      assertEquals("MessagePinned", forB.get("type").asText());
      assertEquals(m2, forB.get("message").get("id").asText());
      assertFalse(forB.get("message").has("replyToId"));
      assertFalse(forB.get("message").has("repliedMessage"));
      assertFalse(forB.toString().contains("needle-one"));
    }

    @Test
    @DisplayName("MessageEdited reaches only members who can see the edited message")
    void editSplit() throws Exception {
      String queueA = bindQueue(aUserId);
      String queueB = bindQueue(bUserId);
      String queueC = bindQueue(cUserId);
      Map<String, String> edit = Map.of("text", "edited-text");

      assertEquals(
          200,
          putJson(String.format("/rooms/%s/messages/%s/edit", roomId, m1), edit, aToken)
              .getStatus());
      assertEquals(
          200,
          putJson(String.format("/rooms/%s/messages/%s/edit", roomId, m2), edit, aToken)
              .getStatus());

      for (String queue : List.of(queueA, queueC)) {
        assertEquals(m1, nextEvent(queue).get("messageId").asText());
        assertEquals(m2, nextEvent(queue).get("messageId").asText());
      }
      assertEquals(m2, nextEvent(queueB).get("messageId").asText());
    }
  }

  @Nested
  @DisplayName("How cleared_at is written")
  class ClearedAtWrites {

    private MockHttpResponse addMember(UUID user, boolean historyCleared) throws Exception {
      return dispatcher.post(
          String.format("/rooms/%s/members", roomId),
          objectMapper.writeValueAsString(
              List.of(
                  Map.of(
                      "userId",
                      user.toString(),
                      "owner",
                      false,
                      "historyCleared",
                      historyCleared))),
          aToken);
    }

    @Test
    @DisplayName("Adding a member with history hidden stores a database-clock cleared_at")
    void addHidden() throws Exception {
      UUID hidden = MockedAccount.getAccount(MockedAccountType.SCHROEDER).getUUID();
      MockHttpResponse response = addMember(hidden, true);
      assertEquals(201, response.getStatus(), response.getContentAsString());

      var settings = integrationTestUtils.getRoomUserSettings(roomId, hidden);
      assertTrue(settings.isPresent());
      assertEquals(
          settings.get().getClearedAt().toInstant(),
          OffsetDateTime.parse(
                  objectMapper
                      .readTree(response.getContentAsString())
                      .get(0)
                      .get("clearedAt")
                      .asText())
              .toInstant());
    }

    @Test
    @DisplayName("Adding a member with history shown writes no cleared_at")
    void addShown() throws Exception {
      UUID shown = MockedAccount.getAccount(MockedAccountType.MARCIE_JOHNSON).getUUID();
      assertEquals(201, addMember(shown, false).getStatus());
      assertTrue(
          integrationTestUtils
              .getRoomUserSettings(roomId, shown)
              .map(RoomUserSettings::getClearedAt)
              .isEmpty());
    }

    @Test
    @DisplayName("Clear history returns the stored value and a message after it is visible")
    void manualClear() throws Exception {
      MockHttpResponse response =
          dispatcher.put(String.format("/rooms/%s/clear", roomId), null, cToken);
      assertEquals(200, response.getStatus(), response.getContentAsString());
      OffsetDateTime returned =
          OffsetDateTime.parse(
              objectMapper.readTree(response.getContentAsString()).get("clearedAt").asText());
      OffsetDateTime stored =
          integrationTestUtils.getRoomUserSettings(roomId, cUserId).orElseThrow().getClearedAt();
      assertEquals(stored.toInstant(), returned.toInstant());

      assertEquals(
          201,
          dispatcher
              .post(
                  String.format("/rooms/%s/messages", roomId),
                  objectMapper.writeValueAsString(Map.of("text", "after clear")),
                  cToken)
              .getStatus());
      List<JsonNode> items = timelineItems("", cToken);
      assertEquals(
          1, items.stream().filter(i -> i.has("message") && !i.get("message").isNull()).count());
    }
  }

  @Nested
  @DisplayName("Live creation with the database clock")
  class LiveRoomCreation {

    @Test
    @DisplayName("Room creator sees ROOM_CREATED and their first message, and can reply to it")
    void creatorSeesOwnBeginning() throws Exception {
      clock.removeFixTime();
      MockHttpResponse created =
          dispatcher.post(
              "/rooms",
              objectMapper.writeValueAsString(
                  Map.of(
                      "name",
                      "realistic",
                      "description",
                      "d",
                      "type",
                      "GROUP",
                      "members",
                      List.of(
                          Map.of("userId", bUserId.toString(), "owner", false),
                          Map.of(
                              "userId",
                              MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT)
                                  .getUUID()
                                  .toString(),
                              "owner",
                              false)))),
              aToken);
      assertEquals(201, created.getStatus(), created.getContentAsString());
      String newRoomId = objectMapper.readTree(created.getContentAsString()).get("id").asText();

      MockHttpResponse sent =
          dispatcher.post(
              String.format("/rooms/%s/messages", newRoomId),
              objectMapper.writeValueAsString(Map.of("text", "first")),
              aToken);
      assertEquals(201, sent.getStatus());
      String firstId = objectMapper.readTree(sent.getContentAsString()).get("id").asText();

      JsonNode root = json(dispatcher.get(String.format("/rooms/%s/timeline", newRoomId), aToken));
      List<String> types = new ArrayList<>();
      List<String> messageIds = new ArrayList<>();
      root.get("items")
          .forEach(
              i -> {
                if (i.has("systemEvent") && !i.get("systemEvent").isNull()) {
                  types.add(i.get("systemEvent").get("type").asText());
                } else {
                  messageIds.add(i.get("message").get("id").asText());
                }
              });
      assertTrue(types.contains("ROOM_CREATED"), "types: " + types);
      assertTrue(messageIds.contains(firstId));

      MockHttpResponse reply =
          dispatcher.post(
              String.format("/rooms/%s/messages", newRoomId),
              objectMapper.writeValueAsString(Map.of("text", "re", "replyToId", firstId)),
              aToken);
      assertEquals(201, reply.getStatus());
    }
  }

  @Nested
  @DisplayName("End to end with the database clock")
  class EndToEnd {

    @Test
    @DisplayName("A member added with history hidden sees its own added line and what follows")
    void addedWithHiddenHistory() throws Exception {
      clock.removeFixTime();
      UUID lucy = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
      UUID hidden = MockedAccount.getAccount(MockedAccountType.SCHROEDER).getUUID();
      String hiddenToken = MockedAccount.getAccount(MockedAccountType.SCHROEDER).getToken();
      MockHttpResponse created =
          dispatcher.post(
              "/rooms",
              objectMapper.writeValueAsString(
                  Map.of(
                      "name",
                      "e2e",
                      "description",
                      "d",
                      "type",
                      "GROUP",
                      "members",
                      List.of(
                          Map.of("userId", bUserId.toString(), "owner", false),
                          Map.of("userId", lucy.toString(), "owner", false)))),
              aToken);
      assertEquals(201, created.getStatus(), created.getContentAsString());
      String newRoomId = objectMapper.readTree(created.getContentAsString()).get("id").asText();

      String oldText = sendText(newRoomId, "old text");
      MockHttpResponse added =
          dispatcher.post(
              String.format("/rooms/%s/members", newRoomId),
              objectMapper.writeValueAsString(
                  List.of(
                      Map.of("userId", hidden.toString(), "owner", false, "historyCleared", true))),
              aToken);
      assertEquals(201, added.getStatus(), added.getContentAsString());
      String newText = sendText(newRoomId, "new text");
      String attachmentMessage = sendAttachment(newRoomId);

      JsonNode root =
          json(dispatcher.get(String.format("/rooms/%s/timeline", newRoomId), hiddenToken));
      List<String> messageIds = new ArrayList<>();
      List<JsonNode> events = new ArrayList<>();
      root.get("items")
          .forEach(
              i -> {
                if (i.has("systemEvent") && !i.get("systemEvent").isNull()) {
                  events.add(i.get("systemEvent"));
                } else {
                  messageIds.add(i.get("message").get("id").asText());
                }
              });

      assertEquals(List.of(newText, attachmentMessage), messageIds);
      assertFalse(messageIds.contains(oldText));
      assertEquals(1, events.size(), events.toString());
      assertEquals("MEMBER_ADDED", events.get(0).get("type").asText());
      assertEquals(
          hidden.toString(), events.get(0).get("content").get("addedUserIds").get(0).asText());

      OffsetDateTime clearedAt =
          integrationTestUtils
              .getRoomUserSettings(UUID.fromString(newRoomId), hidden)
              .orElseThrow()
              .getClearedAt();
      assertTrue(OffsetDateTime.parse(events.get(0).get("createdAt").asText()).isAfter(clearedAt));
    }

    private String sendText(String targetRoomId, String text) throws Exception {
      MockHttpResponse sent =
          dispatcher.post(
              String.format("/rooms/%s/messages", targetRoomId),
              objectMapper.writeValueAsString(Map.of("text", text)),
              aToken);
      assertEquals(201, sent.getStatus(), sent.getContentAsString());
      return objectMapper.readTree(sent.getContentAsString()).get("id").asText();
    }

    private String sendAttachment(String targetRoomId) throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      storageMockServer.mockUpload(
          fileMock,
          new StorageMockServer.UploadResponse()
              .digest("")
              .digestAlgorithm("")
              .size(fileMock.getSize()),
          true);
      UUID targetRoom = UUID.fromString(targetRoomId);
      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(targetRoomId)).thenReturn(targetRoom);
        uuid.when(() -> UUID.fromString(aUserId.toString())).thenReturn(aUserId);
        response =
            dispatcher.postMultipart(
                String.format("/rooms/%s/attachments", targetRoomId),
                fileMock.getFileBytes(),
                Map.of(
                    "mimeType",
                    fileMock.getMimeType(),
                    "contentLength",
                    String.valueOf(fileMock.getSize())),
                StringFormatUtils.encodeToUtf8(fileMock.getName()),
                ContentType.parse(fileMock.getMimeType()),
                aToken);
      }
      assertEquals(201, response.getStatus(), response.getContentAsString());
      JsonNode items =
          json(dispatcher.get(String.format("/rooms/%s/timeline", targetRoomId), aToken))
              .get("items");
      String attachmentMessageId = null;
      for (JsonNode item : items) {
        JsonNode message = item.get("message");
        if (message != null && !message.isNull() && message.has("attachment")) {
          attachmentMessageId = message.get("id").asText();
        }
      }
      assertTrue(attachmentMessageId != null, "attachment message not in the timeline");
      return attachmentMessageId;
    }
  }

  @Nested
  @DisplayName("Creation timeline")
  class CreationTimeline {

    private String create(String type, List<UUID> members) throws Exception {
      clock.removeFixTime();
      MockHttpResponse created =
          dispatcher.post(
              "/rooms",
              objectMapper.writeValueAsString(
                  "ONE_TO_ONE".equals(type)
                      ? Map.of("type", type, "members", memberMaps(members))
                      : Map.of(
                          "name",
                          "creation",
                          "description",
                          "d",
                          "type",
                          type,
                          "members",
                          memberMaps(members))),
              aToken);
      assertEquals(201, created.getStatus(), created.getContentAsString());
      return objectMapper.readTree(created.getContentAsString()).get("id").asText();
    }

    private List<Map<String, Object>> memberMaps(List<UUID> members) {
      return members.stream()
          .map(id -> Map.<String, Object>of("userId", id.toString(), "owner", false))
          .toList();
    }

    private List<JsonNode> events(String newRoomId, String token) throws Exception {
      JsonNode root = json(dispatcher.get(String.format("/rooms/%s/timeline", newRoomId), token));
      List<JsonNode> events = new ArrayList<>();
      root.get("items").forEach(i -> events.add(i.get("systemEvent")));
      return events;
    }

    @Test
    @DisplayName("Group creation shows ROOM_CREATED then one MEMBER_ADDED per member to everyone")
    void groupCreation() throws Exception {
      UUID lucyId = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
      String lucyToken = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getToken();
      String newRoomId = create("GROUP", List.of(bUserId, lucyId));

      for (String token : List.of(aToken, bToken, lucyToken)) {
        List<JsonNode> events = events(newRoomId, token);
        assertEquals(3, events.size(), events.toString());
        assertEquals("ROOM_CREATED", events.get(0).get("type").asText());
        assertEquals("MEMBER_ADDED", events.get(1).get("type").asText());
        assertEquals("MEMBER_ADDED", events.get(2).get("type").asText());
        for (JsonNode event : events) {
          assertEquals(aUserId.toString(), event.get("actorId").asText());
        }
        assertEquals(
            aUserId.toString(), events.get(1).get("content").get("addedByUserId").asText());
        assertEquals(
            aUserId.toString(), events.get(2).get("content").get("addedByUserId").asText());
        assertEquals(
            List.of(bUserId.toString()),
            List.of(events.get(1).get("content").get("addedUserIds").get(0).asText()));
        assertEquals(1, events.get(1).get("content").get("addedUserIds").size());
        assertEquals(
            lucyId.toString(), events.get(2).get("content").get("addedUserIds").get(0).asText());
        assertEquals(1, events.get(2).get("content").get("addedUserIds").size());
      }

      OffsetDateTime roomCreatedAt =
          systemEventRepository.findByRoomId(newRoomId).stream()
              .filter(e -> "ROOM_CREATED".equals(e.getType()))
              .findFirst()
              .orElseThrow()
              .getCreatedAt();
      OffsetDateTime lastCreationLineAt =
          systemEventRepository.findByRoomId(newRoomId).stream()
              .map(SystemEvent::getCreatedAt)
              .max(Comparator.naturalOrder())
              .orElseThrow();
      for (UUID member : List.of(aUserId, bUserId, lucyId)) {
        OffsetDateTime joinedAt =
            subscriptionRepository
                .getById(newRoomId, member.toString())
                .orElseThrow()
                .getJoinedAt();
        assertEquals(lastCreationLineAt.toInstant(), joinedAt.toInstant());
      }
      assertEquals(
          roomCreatedAt.toInstant(),
          integrationTestUtils
              .getRoomById(UUID.fromString(newRoomId))
              .orElseThrow()
              .getCreatedAt()
              .toInstant());
    }

    @Test
    @DisplayName("Temporary creation shows ROOM_CREATED then one MEMBER_ADDED per member")
    void temporaryCreation() throws Exception {
      String newRoomId = create("TEMPORARY", List.of(bUserId));

      List<JsonNode> events = events(newRoomId, bToken);
      assertEquals(2, events.size(), events.toString());
      assertEquals("ROOM_CREATED", events.get(0).get("type").asText());
      assertEquals("MEMBER_ADDED", events.get(1).get("type").asText());
      assertEquals(
          bUserId.toString(), events.get(1).get("content").get("addedUserIds").get(0).asText());
    }

    @Test
    @DisplayName("One-to-one creation has ROOM_CREATED only")
    void oneToOneCreation() throws Exception {
      String newRoomId = create("ONE_TO_ONE", List.of(bUserId));

      List<JsonNode> events = events(newRoomId, aToken);
      assertEquals(1, events.size(), events.toString());
      assertEquals("ROOM_CREATED", events.get(0).get("type").asText());
      assertEquals(aUserId.toString(), events.get(0).get("actorId").asText());
    }
  }
}
