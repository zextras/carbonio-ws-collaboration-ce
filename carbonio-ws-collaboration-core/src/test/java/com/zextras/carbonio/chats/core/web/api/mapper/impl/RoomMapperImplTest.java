// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.web.api.mapper.RoomMapper;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class RoomMapperImplTest {

  private final RoomMapper roomMapper;

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private Room group;
  private Room oneToOne;
  private Room temporary;

  RoomMapperImplTest(RoomMapper roomMapper) {
    this.roomMapper = roomMapper;
  }

  @BeforeEach
  void init() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    group =
        Room.create()
            .id(UUID.randomUUID().toString())
            .type(RoomType.GROUP)
            .name("group")
            .description("Group room")
            .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"));
    group.subscriptions(
        List.of(
            Subscription.create(group, user1Id.toString()).owner(true),
            Subscription.create(group, user2Id.toString()).owner(false),
            Subscription.create(group, user3Id.toString()).owner(false)));
    oneToOne = Room.create().id(UUID.randomUUID().toString()).type(RoomType.ONE_TO_ONE);
    oneToOne.subscriptions(
        List.of(
            Subscription.create(oneToOne, user1Id.toString()).owner(true),
            Subscription.create(oneToOne, user2Id.toString()).owner(true)));
    temporary = Room.create().id(UUID.randomUUID().toString()).type(RoomType.TEMPORARY);
    temporary.subscriptions(List.of(Subscription.create(temporary, user1Id.toString())));
  }

  @Test
  @DisplayName("Every domain room type maps to the generated type with the same constant name")
  void ent2dto_mapsEveryRoomType() {
    assertEquals(RoomTypeDto.GROUP, roomMapper.ent2dto(group, null, false, false).getType());
    assertEquals(
        RoomTypeDto.ONE_TO_ONE, roomMapper.ent2dto(oneToOne, null, false, false).getType());
    assertEquals(
        RoomTypeDto.TEMPORARY, roomMapper.ent2dto(temporary, null, false, false).getType());
    for (RoomType type : RoomType.values()) {
      assertEquals(type.name(), RoomTypeDto.valueOf(type.name()).name());
    }
    assertEquals(RoomType.values().length, RoomTypeDto.values().length);
  }

  @Test
  @DisplayName("A room without members and settings maps its fields only")
  void ent2dto_basicRoom() {
    List<RoomDto> rooms =
        roomMapper.ent2dto(
            List.of(group, oneToOne), (Map<String, RoomUserSettings>) null, false, false);

    assertEquals(2, rooms.size());
    assertEquals(UUID.fromString(group.getId()), rooms.get(0).getId());
    assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
    assertEquals(UUID.fromString(oneToOne.getId()), rooms.get(1).getId());
    assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
    assertNull(rooms.get(0).getMembers());
    assertNull(rooms.get(1).getMembers());
    assertNull(rooms.get(0).getUserSettings());
    assertNull(rooms.get(1).getUserSettings());
    assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), rooms.get(0).getPictureUpdatedAt());
    assertNull(rooms.get(1).getPictureUpdatedAt());
  }

  @Test
  @DisplayName("Members are mapped only when requested")
  void ent2dto_withMembers() {
    List<RoomDto> rooms =
        roomMapper.ent2dto(
            List.of(group, oneToOne, temporary), (Map<String, RoomUserSettings>) null, true, false);

    assertEquals(3, rooms.size());
    assertEquals(RoomTypeDto.TEMPORARY, rooms.get(2).getType());
    assertNotNull(rooms.get(0).getMembers());
    assertNotNull(rooms.get(1).getMembers());
    assertNotNull(rooms.get(2).getMembers());
    assertNull(rooms.get(0).getUserSettings());
    assertNull(rooms.get(1).getUserSettings());
    assertNull(rooms.get(2).getUserSettings());
    List<MemberDto> members = rooms.get(0).getMembers();
    assertEquals(3, members.size());
    assertEquals(user1Id, members.get(0).getUserId());
    assertTrue(members.get(0).isOwner());
    assertFalse(members.get(1).isOwner());
  }

  @Test
  @DisplayName("Settings are looked up by room identifier and default to not muted")
  void ent2dto_withSettings() {
    Map<String, RoomUserSettings> settings =
        Map.of(
            group.getId(),
            RoomUserSettings.create(group, user1Id.toString()).mutedUntil(OffsetDateTime.now()));

    List<RoomDto> rooms = roomMapper.ent2dto(List.of(group, oneToOne), settings, false, true);

    assertNull(rooms.get(0).getMembers());
    assertNull(rooms.get(1).getMembers());
    assertNotNull(rooms.get(0).getUserSettings());
    assertNotNull(rooms.get(1).getUserSettings());
    assertTrue(rooms.get(0).getUserSettings().isMuted());
    assertFalse(rooms.get(1).getUserSettings().isMuted());
  }

  @Test
  @DisplayName("A complete room carries members and settings")
  void ent2dto_completeRoom() {
    OffsetDateTime clearedAt = OffsetDateTime.parse("2022-01-02T00:00:00Z");
    RoomUserSettings settings =
        RoomUserSettings.create(group, user2Id.toString())
            .mutedUntil(OffsetDateTime.now())
            .clearedAt(clearedAt);

    RoomDto room = roomMapper.ent2dto(group, settings, true, true);

    assertEquals(UUID.fromString(group.getId()), room.getId());
    assertEquals(2, room.getMembers().stream().filter(m -> !m.isOwner()).count());
    assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
    assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
    assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());
    assertTrue(room.getUserSettings().isMuted());
    assertEquals(clearedAt, room.getUserSettings().getClearedAt());
  }

  @Test
  @DisplayName("Without any user settings the room is not muted")
  void ent2dto_memberWithoutSettings() {
    RoomDto room =
        roomMapper.ent2dto(group, Optional.<RoomUserSettings>empty().orElse(null), true, true);

    assertEquals(3, room.getMembers().size());
    assertNotNull(room.getUserSettings());
    assertFalse(room.getUserSettings().isMuted());
    assertNull(room.getUserSettings().getClearedAt());
  }

  @Test
  @DisplayName("A missing room maps to null")
  void ent2dto_nullRoom() {
    assertNull(roomMapper.ent2dto((Room) null, null, true, true));
  }
}
