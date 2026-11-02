// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class TimedUpdateIT {

  private final Database database;
  private final IntegrationTestUtils integrationTestUtils;

  public TimedUpdateIT(Database database, IntegrationTestUtils integrationTestUtils) {
    this.database = database;
    this.integrationTestUtils = integrationTestUtils;
  }

  private OffsetDateTime dbNow() {
    return database
        .sqlQuery("SELECT clock_timestamp() AS now")
        .findOne()
        .getTimestamp("now")
        .toInstant()
        .atOffset(ZoneOffset.UTC);
  }

  private OffsetDateTime column(String sql, String p1, String p2) {
    var query = database.sqlQuery(sql).setParameter("p1", p1);
    if (p2 != null) {
      query.setParameter("p2", p2);
    }
    var ts = query.findOne().getTimestamp("v");
    return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
  }

  @Test
  @DisplayName("Embedded id participant: flag changed, updated_at stamped by the database")
  void participant_flagChange() {
    UUID room = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        room, RoomTypeDto.GROUP, "room", List.of(UUID.randomUUID()));
    String roomId = room.toString();
    String meetingId = UUID.randomUUID().toString();
    database.save(
        Meeting.create()
            .id(meetingId)
            .name("m")
            .meetingType(MeetingType.PERMANENT)
            .roomId(roomId)
            .active(false));
    Meeting meeting = database.find(Meeting.class, meetingId);
    database.save(Participant.create(meeting, "user-1").queueId("queue-1"));
    String sql =
        "SELECT updated_at AS v FROM chats.participant WHERE meeting_id = :p1 AND user_id = :p2";
    OffsetDateTime createdAt =
        column(
            "SELECT created_at AS v FROM chats.participant WHERE meeting_id = :p1 AND user_id ="
                + " :p2",
            meetingId,
            "user-1");
    OffsetDateTime before = dbNow();

    Participant participant =
        database.find(Participant.class, Participant.create(meeting, "user-1").getId());
    participant.audioStreamOn(true);
    assertEquals(
        java.util.Set.of("audioStreamOn"), database.beanState(participant).dirtyValues().keySet());
    TimedUpdate.update(database, participant, TimedUpdate.UPDATED_AT_NOW);

    Participant reloaded =
        database
            .find(Participant.class)
            .setDisableLazyLoading(false)
            .where()
            .idEq(participant.getId())
            .setUseCache(false)
            .findOne();
    assertTrue(reloaded.hasAudioStreamOn());
    assertFalse(reloaded.hasVideoStreamOn());
    assertEquals("queue-1", reloaded.getQueueId());
    OffsetDateTime updatedAt = column(sql, meetingId, "user-1");
    assertNotNull(updatedAt);
    assertFalse(updatedAt.isBefore(before));
    assertTrue(updatedAt.isAfter(createdAt));
  }

  @Test
  @DisplayName("Embedded id subscription: owner flag changed, association left alone")
  void subscription_ownerChange() {
    UUID roomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Room room =
        integrationTestUtils.generateAndSaveRoom(
            roomId, RoomTypeDto.GROUP, "room", List.of(userId));
    assertTrue(
        database
            .find(Subscription.class, new SubscriptionId(roomId.toString(), userId.toString()))
            .isOwner());
    SubscriptionId id = new SubscriptionId(roomId.toString(), userId.toString());
    OffsetDateTime before = dbNow();

    Subscription subscription = database.find(Subscription.class, id);
    subscription.owner(false);
    assertEquals(
        java.util.Set.of("isOwner"), database.beanState(subscription).dirtyValues().keySet());
    TimedUpdate.update(database, subscription, TimedUpdate.UPDATED_AT_NOW);

    var row =
        database
            .sqlQuery(
                "SELECT owner, external, temporary, updated_at, created_at FROM chats.subscription"
                    + " WHERE room_id = :r AND user_id = :u")
            .setParameter("r", roomId.toString())
            .setParameter("u", userId.toString())
            .findOne();
    assertFalse(row.getBoolean("owner"));
    assertFalse(row.getBoolean("external"));
    assertFalse(row.getTimestamp("updated_at").toInstant().isBefore(before.toInstant()));
    assertTrue(
        row.getTimestamp("updated_at")
            .toInstant()
            .isAfter(row.getTimestamp("created_at").toInstant()));
    assertNotNull(room);
  }

  @Test
  @DisplayName("A new bean is rejected")
  void newBean_rejected() {
    Room room = Room.create().id(UUID.randomUUID().toString()).name("n");
    assertThrows(
        IllegalArgumentException.class,
        () -> TimedUpdate.update(database, room, TimedUpdate.UPDATED_AT_NOW));
  }

  @Test
  @DisplayName("A dirty collection or association property is rejected")
  void nonScalarProperty_rejected() {
    UUID roomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(userId));
    Room room = database.find(Room.class, roomId.toString());
    room.subscriptions(new java.util.ArrayList<>());
    assertThrows(
        IllegalArgumentException.class,
        () -> TimedUpdate.update(database, room, TimedUpdate.UPDATED_AT_NOW));

    Subscription subscription =
        database.find(Subscription.class, new SubscriptionId(roomId.toString(), userId.toString()));
    subscription.room(database.find(Room.class, roomId.toString()));
    if (database.beanState(subscription).dirtyValues().containsKey("room")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> TimedUpdate.update(database, subscription, TimedUpdate.UPDATED_AT_NOW));
    }
  }

  @Test
  @DisplayName("Updating a row deleted in the meantime throws EntityNotFoundException")
  void missingRow_throws() {
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "room", List.of(UUID.randomUUID()));
    Room room = database.find(Room.class, roomId.toString());
    room.name("renamed");
    database
        .sqlUpdate("DELETE FROM chats.subscription WHERE room_id = :id")
        .setParameter("id", roomId.toString())
        .execute();
    database
        .sqlUpdate("DELETE FROM chats.room WHERE id = :id")
        .setParameter("id", roomId.toString())
        .execute();
    assertThrows(
        jakarta.persistence.EntityNotFoundException.class,
        () -> TimedUpdate.update(database, room, TimedUpdate.UPDATED_AT_NOW));
  }

  @Test
  @DisplayName("A property nulled on the bean becomes a NULL column")
  void room_nullAssignment() {
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId,
        RoomTypeDto.GROUP,
        "room",
        null,
        List.of(UUID.randomUUID()),
        List.of(),
        null,
        OffsetDateTime.parse("2020-01-01T00:00:00Z"));
    String sql = "SELECT picture_updated_at AS v FROM chats.room WHERE id = :p1";
    assertNotNull(column(sql, roomId.toString(), null));

    Room room = database.find(Room.class, roomId.toString());
    room.pictureUpdatedAt(null);
    TimedUpdate.update(database, room);

    assertNull(column(sql, roomId.toString(), null));
  }

  @Test
  @DisplayName("Raw time assignments on several columns are stamped by the database")
  void room_rawTimes() {
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "room", List.of(UUID.randomUUID()));
    OffsetDateTime before = dbNow();

    Room room = database.find(Room.class, roomId.toString());
    room.name("renamed");
    TimedUpdate.update(
        database, room, "pictureUpdatedAt = clock_timestamp()", TimedUpdate.UPDATED_AT_NOW);

    String id = roomId.toString();
    OffsetDateTime picture =
        column("SELECT picture_updated_at AS v FROM chats.room WHERE id = :p1", id, null);
    OffsetDateTime updatedAt =
        column("SELECT updated_at AS v FROM chats.room WHERE id = :p1", id, null);
    assertFalse(picture.isBefore(before));
    assertFalse(updatedAt.isBefore(before));
    assertEquals(
        "renamed",
        database
            .sqlQuery("SELECT name FROM chats.room WHERE id = :p1")
            .setParameter("p1", id)
            .findOne()
            .getString("name"));
  }
}
