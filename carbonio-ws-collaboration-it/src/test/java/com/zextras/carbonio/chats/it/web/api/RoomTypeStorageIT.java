// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import io.ebean.Database;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class RoomTypeStorageIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final RoomRepository roomRepository;
  private final Database database;

  RoomTypeStorageIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      RoomRepository roomRepository,
      Database database) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.roomRepository = roomRepository;
    this.database = database;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  private final UUID userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
  private final String userToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();

  private UUID insertRowWithStoredType(String storedType, boolean memberOfRoom) {
    UUID roomId = UUID.randomUUID();
    database
        .sqlUpdate(
            "insert into chats.room (id, name, type, created_at, updated_at)"
                + " values (:id, :name, :type, now(), now())")
        .setParameter("id", roomId.toString())
        .setParameter("name", "legacy-" + storedType)
        .setParameter("type", storedType)
        .execute();
    if (memberOfRoom) {
      database
          .sqlUpdate(
              "insert into chats.subscription (user_id, room_id, joined_at, owner, created_at,"
                  + " updated_at) values (:user, :room, now(), true, now(), now())")
          .setParameter("user", userId.toString())
          .setParameter("room", roomId.toString())
          .execute();
    }
    return roomId;
  }

  private String storedType(UUID roomId) {
    return database
        .sqlQuery("select type from chats.room where id = :id")
        .setParameter("id", roomId.toString())
        .findOne()
        .getString("type");
  }

  @Test
  @DisplayName("Rows stored with the constant names load as the domain room type")
  void existingRows_loadAsDomainEnum() {
    UUID group = insertRowWithStoredType("GROUP", true);
    UUID oneToOne = insertRowWithStoredType("ONE_TO_ONE", true);
    UUID temporary = insertRowWithStoredType("TEMPORARY", true);

    assertEquals(RoomType.GROUP, roomRepository.getById(group.toString()).orElseThrow().getType());
    assertEquals(
        RoomType.ONE_TO_ONE, roomRepository.getById(oneToOne.toString()).orElseThrow().getType());
    assertEquals(
        RoomType.TEMPORARY, roomRepository.getById(temporary.toString()).orElseThrow().getType());
  }

  @Test
  @DisplayName("The REST output of rows stored with the old values keeps the lower case types")
  void existingRows_restOutputUnchanged() throws Exception {
    UUID group = insertRowWithStoredType("GROUP", true);
    UUID oneToOne = insertRowWithStoredType("ONE_TO_ONE", true);
    UUID temporary = insertRowWithStoredType("TEMPORARY", true);

    for (UUID roomId : new UUID[] {group, oneToOne, temporary}) {
      MockHttpResponse response = dispatcher.get("/rooms/" + roomId, userToken);
      assertEquals(200, response.getStatus());
      JsonNode body = objectMapper.readTree(response.getContentAsString());
      String expected =
          switch (storedType(roomId)) {
            case "GROUP" -> "group";
            case "ONE_TO_ONE" -> "one_to_one";
            default -> "temporary";
          };
      assertEquals(expected, body.get("type").asText());
      assertEquals(roomId.toString(), body.get("id").asText());
    }

    MockHttpResponse list = dispatcher.get("/rooms", userToken);
    assertEquals(200, list.getStatus());
    JsonNode rooms = objectMapper.readTree(list.getContentAsString());
    for (UUID roomId : new UUID[] {group, oneToOne, temporary}) {
      boolean found = false;
      for (JsonNode room : rooms) {
        if (roomId.toString().equals(room.get("id").asText())) {
          found = true;
        }
      }
      assertTrue(found, "room " + roomId + " missing from the list");
    }
  }

  @Test
  @DisplayName("Saving a room stores the constant name of the domain room type")
  void savedRooms_storeConstantNames() {
    for (RoomType type : RoomType.values()) {
      UUID roomId = UUID.randomUUID();
      roomRepository.insert(Room.create().id(roomId.toString()).name("n").type(type));

      assertEquals(type.name(), storedType(roomId));
    }
  }

  @Test
  @DisplayName("The one-to-one lookup finds a row stored with the old value")
  void oneToOneLookup_findsExistingRow() {
    UUID otherUser = UUID.randomUUID();
    UUID roomId = insertRowWithStoredType("ONE_TO_ONE", true);
    database
        .sqlUpdate(
            "insert into chats.subscription (user_id, room_id, joined_at, owner, created_at,"
                + " updated_at) values (:user, :room, now(), true, now(), now())")
        .setParameter("user", otherUser.toString())
        .setParameter("room", roomId.toString())
        .execute();

    assertEquals(
        roomId.toString(),
        roomRepository
            .getOneToOneByAllUserIds(userId.toString(), otherUser.toString())
            .orElseThrow()
            .getId());
  }
}
