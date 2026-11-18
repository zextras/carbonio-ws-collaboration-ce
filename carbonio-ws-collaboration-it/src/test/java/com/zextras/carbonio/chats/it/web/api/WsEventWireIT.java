// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class WsEventWireIT {

  private static final Pattern UUID_PATTERN =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  private static final Pattern TIMESTAMP_PATTERN =
      Pattern.compile(
          "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?(Z|[+-]\\d{2}:\\d{2})");

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils utils;
  private final Database database;
  private final Channel channel;

  private final UUID snoopy = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
  private final String snoopyToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
  private final UUID charlie = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
  private final String charlieToken =
      MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
  private final UUID lucy = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();

  private final Map<UUID, String> queues = new LinkedHashMap<>();
  private final Map<String, String> aliases = new LinkedHashMap<>();
  private final List<String> transcript = new ArrayList<>();

  public WsEventWireIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils utils,
      Database database,
      Channel channel) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.utils = utils;
    this.database = database;
    this.channel = channel;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @Test
  @DisplayName("The serialized WebSocket events keep their exact wire shape")
  void wireShape() throws Exception {
    UUID room = UUID.randomUUID();
    UUID target = UUID.randomUUID();
    utils.generateAndSaveRoom(
        room,
        RoomTypeDto.GROUP,
        "wire",
        List.of(snoopy, charlie, lucy),
        List.of(snoopy),
        null,
        null);
    utils.generateAndSaveRoom(
        target,
        RoomTypeDto.GROUP,
        "target",
        List.of(snoopy, charlie, lucy),
        List.of(snoopy),
        null,
        null);
    alias(snoopy.toString(), "SNOOPY");
    alias(charlie.toString(), "CHARLIE");
    alias(lucy.toString(), "LUCY");
    alias(room.toString(), "ROOM");
    alias(target.toString(), "TARGET");
    for (UUID user : List.of(snoopy, charlie, lucy)) {
      queues.put(user, bindQueue(user));
    }

    String m1 = send(room, "{\"text\":\"first\",\"tempId\":\"temp-1\"}");
    capture("send with tempId");
    String m2 = send(room, "{\"text\":\"second\"}");
    capture("send plain");

    database
        .sqlUpdate(
            "INSERT INTO chats.room_user_settings (user_id, room_id, cleared_at, created_at,"
                + " updated_at) VALUES (:u, :r, clock_timestamp(), clock_timestamp(),"
                + " clock_timestamp())")
        .setParameter("u", lucy.toString())
        .setParameter("r", room.toString())
        .execute();

    String reply1 = send(room, "{\"text\":\"reply hidden\",\"replyToId\":\"" + m1 + "\"}");
    capture("reply to a message hidden from one member");
    String m3 = send(room, "{\"text\":\"third\"}");
    capture("send after clear");
    String reply2 = send(room, "{\"text\":\"reply visible\",\"replyToId\":\"" + m3 + "\"}");
    capture("reply to a message visible to all");

    put(room, "/messages/" + reply1 + "/pin", null, snoopyToken);
    capture("pin a reply");
    delete(room, "/messages/" + reply1 + "/pin", snoopyToken);
    capture("unpin");

    put(room, "/messages/" + m2 + "/edit", "{\"text\":\"second edited\"}", snoopyToken);
    capture("edit");
    put(room, "/messages/" + m3 + "/pin", null, snoopyToken);
    capture("pin plain");
    put(room, "/messages/" + m3 + "/edit", "{\"text\":\"third edited\"}", snoopyToken);
    capture("edit a pinned message");

    put(room, "/messages/" + m3 + "/reaction", "{\"reaction\":\"up\"}", charlieToken);
    capture("set reaction");
    delete(room, "/messages/" + m3 + "/reaction", charlieToken);
    capture("remove reaction");

    put(room, "/read", "{\"itemId\":\"" + reply2 + "\"}", charlieToken);
    capture("read marker");

    post(room, "/messages/" + reply2 + "/forward", "{\"toRoomId\":\"" + target + "\"}");
    capture("forward");

    delete(room, "/messages/" + m2, snoopyToken);
    capture("delete");
    delete(room, "/messages/" + m3, snoopyToken);
    capture("delete a pinned message");

    assertEquals(EXPECTED.strip(), String.join("\n", transcript));
  }

  private void alias(String id, String name) {
    aliases.put(id, name);
  }

  private String send(UUID room, String body) throws Exception {
    MockHttpResponse response =
        dispatcher.post(String.format("/rooms/%s/messages", room), body, snoopyToken);
    assertEquals(201, response.getStatus());
    return objectMapper.readTree(response.getContentAsString()).get("id").asText();
  }

  private void put(UUID room, String path, String body, String token) throws Exception {
    assertSuccess(dispatcher.put(String.format("/rooms/%s%s", room, path), body, token));
  }

  private void delete(UUID room, String path, String token) throws Exception {
    assertSuccess(dispatcher.delete(String.format("/rooms/%s%s", room, path), token));
  }

  private void post(UUID room, String path, String body) throws Exception {
    assertSuccess(dispatcher.post(String.format("/rooms/%s%s", room, path), body, snoopyToken));
  }

  private void assertSuccess(MockHttpResponse response) throws Exception {
    assertEquals(2, response.getStatus() / 100, response.getContentAsString());
  }

  private String bindQueue(UUID user) throws Exception {
    channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
    String queue = channel.queueDeclare().getQueue();
    channel.queueBind(queue, user.toString(), "user-events");
    return queue;
  }

  private void capture(String step) throws Exception {
    transcript.add("# " + step);
    for (Map.Entry<UUID, String> queue : queues.entrySet()) {
      GetResponse response = poll(queue.getValue());
      while (response != null) {
        transcript.add(
            aliases.get(queue.getKey().toString())
                + " <- "
                + normalize(new String(response.getBody(), StandardCharsets.UTF_8)));
        response = poll(queue.getValue());
      }
    }
  }

  private GetResponse poll(String queue) throws Exception {
    for (int i = 0; i < 4; i++) {
      GetResponse response = channel.basicGet(queue, true);
      if (response != null) {
        return response;
      }
      Thread.sleep(100);
    }
    return null;
  }

  private String normalize(String json) {
    Matcher uuids = UUID_PATTERN.matcher(json);
    StringBuilder out = new StringBuilder();
    while (uuids.find()) {
      String alias = aliases.computeIfAbsent(uuids.group(), key -> "id" + (aliases.size() - 4));
      uuids.appendReplacement(out, "<" + alias + ">");
    }
    uuids.appendTail(out);
    return TIMESTAMP_PATTERN.matcher(out).replaceAll("<ts>");
  }

  private static final String EXPECTED =
      """
      # send with tempId
      SNOOPY <- {"type":"MessageReceived","message":{"id":"<id1>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"first","createdAt":"<ts>","reactions":[]},"tempId":"temp-1"}
      CHARLIE <- {"type":"MessageReceived","message":{"id":"<id1>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"first","createdAt":"<ts>","reactions":[]},"tempId":"temp-1"}
      LUCY <- {"type":"MessageReceived","message":{"id":"<id1>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"first","createdAt":"<ts>","reactions":[]},"tempId":"temp-1"}
      # send plain
      SNOOPY <- {"type":"MessageReceived","message":{"id":"<id2>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"second","createdAt":"<ts>","reactions":[]}}
      CHARLIE <- {"type":"MessageReceived","message":{"id":"<id2>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"second","createdAt":"<ts>","reactions":[]}}
      LUCY <- {"type":"MessageReceived","message":{"id":"<id2>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"second","createdAt":"<ts>","reactions":[]}}
      # reply to a message hidden from one member
      SNOOPY <- {"type":"MessageReceived","message":{"id":"<id3>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply hidden","createdAt":"<ts>","replyToId":"<id1>","reactions":[],"repliedMessage":{"id":"<id1>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"first","createdAt":"<ts>","reactions":[]}}}
      CHARLIE <- {"type":"MessageReceived","message":{"id":"<id3>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply hidden","createdAt":"<ts>","replyToId":"<id1>","reactions":[],"repliedMessage":{"id":"<id1>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"first","createdAt":"<ts>","reactions":[]}}}
      LUCY <- {"type":"MessageReceived","message":{"id":"<id3>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply hidden","createdAt":"<ts>","reactions":[]}}
      # send after clear
      SNOOPY <- {"type":"MessageReceived","message":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]}}
      CHARLIE <- {"type":"MessageReceived","message":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]}}
      LUCY <- {"type":"MessageReceived","message":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]}}
      # reply to a message visible to all
      SNOOPY <- {"type":"MessageReceived","message":{"id":"<id5>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply visible","createdAt":"<ts>","replyToId":"<id4>","reactions":[],"repliedMessage":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]}}}
      CHARLIE <- {"type":"MessageReceived","message":{"id":"<id5>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply visible","createdAt":"<ts>","replyToId":"<id4>","reactions":[],"repliedMessage":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]}}}
      LUCY <- {"type":"MessageReceived","message":{"id":"<id5>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply visible","createdAt":"<ts>","replyToId":"<id4>","reactions":[],"repliedMessage":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]}}}
      # pin a reply
      SNOOPY <- {"type":"MessagePinned","messageId":"<id3>","roomId":"<ROOM>","pinnedBy":"<SNOOPY>","timestamp":"<ts>","message":{"id":"<id3>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply hidden","createdAt":"<ts>","replyToId":"<id1>","reactions":[],"repliedMessage":{"id":"<id1>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"first","createdAt":"<ts>","reactions":[]}},"systemEventId":"<id6>","systemEventCreatedAt":"<ts>"}
      CHARLIE <- {"type":"MessagePinned","messageId":"<id3>","roomId":"<ROOM>","pinnedBy":"<SNOOPY>","timestamp":"<ts>","message":{"id":"<id3>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply hidden","createdAt":"<ts>","replyToId":"<id1>","reactions":[],"repliedMessage":{"id":"<id1>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"first","createdAt":"<ts>","reactions":[]}},"systemEventId":"<id6>","systemEventCreatedAt":"<ts>"}
      LUCY <- {"type":"MessagePinned","messageId":"<id3>","roomId":"<ROOM>","pinnedBy":"<SNOOPY>","timestamp":"<ts>","message":{"id":"<id3>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"reply hidden","createdAt":"<ts>","reactions":[]},"systemEventId":"<id6>","systemEventCreatedAt":"<ts>"}
      # unpin
      SNOOPY <- {"type":"MessageUnpinned","messageId":"<id3>","roomId":"<ROOM>","unpinnedBy":"<SNOOPY>","timestamp":"<ts>","systemEventId":"<id7>","systemEventCreatedAt":"<ts>"}
      CHARLIE <- {"type":"MessageUnpinned","messageId":"<id3>","roomId":"<ROOM>","unpinnedBy":"<SNOOPY>","timestamp":"<ts>","systemEventId":"<id7>","systemEventCreatedAt":"<ts>"}
      LUCY <- {"type":"MessageUnpinned","messageId":"<id3>","roomId":"<ROOM>","unpinnedBy":"<SNOOPY>","timestamp":"<ts>","systemEventId":"<id7>","systemEventCreatedAt":"<ts>"}
      # edit
      SNOOPY <- {"type":"MessageEdited","messageId":"<id2>","roomId":"<ROOM>","text":"second edited","editedAt":"<ts>"}
      CHARLIE <- {"type":"MessageEdited","messageId":"<id2>","roomId":"<ROOM>","text":"second edited","editedAt":"<ts>"}
      # pin plain
      SNOOPY <- {"type":"MessagePinned","messageId":"<id4>","roomId":"<ROOM>","pinnedBy":"<SNOOPY>","timestamp":"<ts>","message":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]},"systemEventId":"<id8>","systemEventCreatedAt":"<ts>"}
      CHARLIE <- {"type":"MessagePinned","messageId":"<id4>","roomId":"<ROOM>","pinnedBy":"<SNOOPY>","timestamp":"<ts>","message":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]},"systemEventId":"<id8>","systemEventCreatedAt":"<ts>"}
      LUCY <- {"type":"MessagePinned","messageId":"<id4>","roomId":"<ROOM>","pinnedBy":"<SNOOPY>","timestamp":"<ts>","message":{"id":"<id4>","roomId":"<ROOM>","senderId":"<SNOOPY>","text":"third","createdAt":"<ts>","reactions":[]},"systemEventId":"<id8>","systemEventCreatedAt":"<ts>"}
      # edit a pinned message
      SNOOPY <- {"type":"MessageEdited","messageId":"<id4>","roomId":"<ROOM>","text":"third edited","editedAt":"<ts>"}
      CHARLIE <- {"type":"MessageEdited","messageId":"<id4>","roomId":"<ROOM>","text":"third edited","editedAt":"<ts>"}
      LUCY <- {"type":"MessageEdited","messageId":"<id4>","roomId":"<ROOM>","text":"third edited","editedAt":"<ts>"}
      # set reaction
      SNOOPY <- {"type":"ReactionChanged","messageId":"<id4>","roomId":"<ROOM>","userId":"<CHARLIE>","reaction":"up","operation":"added","updatedAt":"<ts>"}
      CHARLIE <- {"type":"ReactionChanged","messageId":"<id4>","roomId":"<ROOM>","userId":"<CHARLIE>","reaction":"up","operation":"added","updatedAt":"<ts>"}
      LUCY <- {"type":"ReactionChanged","messageId":"<id4>","roomId":"<ROOM>","userId":"<CHARLIE>","reaction":"up","operation":"added","updatedAt":"<ts>"}
      # remove reaction
      SNOOPY <- {"type":"ReactionChanged","messageId":"<id4>","roomId":"<ROOM>","userId":"<CHARLIE>","reaction":"up","operation":"removed","deletedAt":"<ts>"}
      CHARLIE <- {"type":"ReactionChanged","messageId":"<id4>","roomId":"<ROOM>","userId":"<CHARLIE>","reaction":"up","operation":"removed","deletedAt":"<ts>"}
      LUCY <- {"type":"ReactionChanged","messageId":"<id4>","roomId":"<ROOM>","userId":"<CHARLIE>","reaction":"up","operation":"removed","deletedAt":"<ts>"}
      # read marker
      SNOOPY <- {"type":"ReadUpdated","roomId":"<ROOM>","userId":"<CHARLIE>","itemCreatedAt":"<ts>"}
      CHARLIE <- {"type":"ReadUpdated","roomId":"<ROOM>","userId":"<CHARLIE>","itemCreatedAt":"<ts>"}
      LUCY <- {"type":"ReadUpdated","roomId":"<ROOM>","userId":"<CHARLIE>","itemCreatedAt":"<ts>"}
      # forward
      SNOOPY <- {"type":"MessageForwarded","message":{"id":"<id9>","roomId":"<TARGET>","senderId":"<SNOOPY>","text":"reply visible","createdAt":"<ts>","forwardedInfo":{"originalSenderId":"<SNOOPY>","originalSentAt":"<ts>"},"reactions":[]}}
      CHARLIE <- {"type":"MessageForwarded","message":{"id":"<id9>","roomId":"<TARGET>","senderId":"<SNOOPY>","text":"reply visible","createdAt":"<ts>","forwardedInfo":{"originalSenderId":"<SNOOPY>","originalSentAt":"<ts>"},"reactions":[]}}
      LUCY <- {"type":"MessageForwarded","message":{"id":"<id9>","roomId":"<TARGET>","senderId":"<SNOOPY>","text":"reply visible","createdAt":"<ts>","forwardedInfo":{"originalSenderId":"<SNOOPY>","originalSentAt":"<ts>"},"reactions":[]}}
      # delete
      SNOOPY <- {"type":"MessageDeleted","messageId":"<id2>","roomId":"<ROOM>","deletedBy":"<SNOOPY>","deletedAt":"<ts>"}
      CHARLIE <- {"type":"MessageDeleted","messageId":"<id2>","roomId":"<ROOM>","deletedBy":"<SNOOPY>","deletedAt":"<ts>"}
      # delete a pinned message
      SNOOPY <- {"type":"MessageDeleted","messageId":"<id4>","roomId":"<ROOM>","deletedBy":"<SNOOPY>","deletedAt":"<ts>"}
      SNOOPY <- {"type":"MessageUnpinned","messageId":"<id4>","roomId":"<ROOM>","unpinnedBy":"<SNOOPY>","timestamp":"<ts>","systemEventId":"<id10>","systemEventCreatedAt":"<ts>"}
      CHARLIE <- {"type":"MessageDeleted","messageId":"<id4>","roomId":"<ROOM>","deletedBy":"<SNOOPY>","deletedAt":"<ts>"}
      CHARLIE <- {"type":"MessageUnpinned","messageId":"<id4>","roomId":"<ROOM>","unpinnedBy":"<SNOOPY>","timestamp":"<ts>","systemEventId":"<id10>","systemEventCreatedAt":"<ts>"}
      LUCY <- {"type":"MessageDeleted","messageId":"<id4>","roomId":"<ROOM>","deletedBy":"<SNOOPY>","deletedAt":"<ts>"}
      LUCY <- {"type":"MessageUnpinned","messageId":"<id4>","roomId":"<ROOM>","unpinnedBy":"<SNOOPY>","timestamp":"<ts>","systemEventId":"<id10>","systemEventCreatedAt":"<ts>"}
      """;
}
