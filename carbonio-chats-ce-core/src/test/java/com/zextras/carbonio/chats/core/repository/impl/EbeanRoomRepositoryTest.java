package com.zextras.carbonio.chats.core.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class EbeanRoomRepositoryTest {

  private EbeanRoomRepository ebeanRoomRepository;
  private Database            database;

  public EbeanRoomRepositoryTest() {
    this.database = mock(Database.class, RETURNS_DEEP_STUBS);
    this.ebeanRoomRepository = new EbeanRoomRepository(this.database);
  }

  @AfterEach
  public void cleanup() {
    reset(database);
  }

  @Nested
  @DisplayName("Gets one-to-one room for both users tests")
  class GetsOneToOneByAllUserIds {

    @Test
    @DisplayName("Retrieves a one-to-one room by both users")
    public void getOneToOneByAllUserIds_testOK() {
      when(database.find(Room.class).where()
        .eq("type", RoomTypeDto.ONE_TO_ONE)
        .and().raw("id in ( " +
          "select distinct a.room_id from chats.subscription a " +
          "inner join chats.subscription b on a.room_id = b.room_id " +
          "and (a.user_id = 'user1Id' or a.user_id = 'user2Id') " +
          "and (b.user_id = 'user1Id' or b.user_id = 'user2Id') " +
          "where (a.user_id = 'user1Id' and b.user_id = 'user2Id') " +
          "or (a.user_id = 'user2Id' and b.user_id = 'user1Id'))")
        .findOneOrEmpty()).thenReturn(Optional.of(Room.create().id("roomId")));

      Optional<Room> room = ebeanRoomRepository.getOneToOneByAllUserIds("user1Id", "user2Id");

      assertTrue(room.isPresent());
      assertEquals("roomId", room.get().getId());
    }

    @Test
    @DisplayName("Returns an empty optional if there isn't a one-to-one room for required users")
    public void getOneToOneByAllUserIds_testNotFound() {
      when(database.find(Room.class).where()
        .eq("type", RoomTypeDto.ONE_TO_ONE)
        .and().raw("id in ( " +
          "select distinct a.room_id from chats.subscription a " +
          "inner join chats.subscription b on a.room_id = b.room_id " +
          "and (a.user_id = 'user1Id' or a.user_id = 'user2Id') " +
          "and (b.user_id = 'user1Id' or b.user_id = 'user2Id') " +
          "where (a.user_id = 'user1Id' and b.user_id = 'user2Id') " +
          "or (a.user_id = 'user2Id' and b.user_id = 'user1Id'))")
        .findOneOrEmpty()).thenReturn(Optional.empty());

      Optional<Room> room = ebeanRoomRepository.getOneToOneByAllUserIds("user1Id", "user2Id");

      assertTrue(room.isEmpty());
    }
  }
}
