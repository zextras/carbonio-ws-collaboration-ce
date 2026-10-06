// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Replaces CE's EbeanRoomRepositoryTest/EbeanUserRepositoryTest (they mocked the Ebean query API)
 * with checks of the Hibernate mappings and queries against the real schema.
 */
@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class JpaRepositoriesTest {

  @Inject DataSource dataSource;
  @Inject RoomRepository roomRepository;
  @Inject UserRepository userRepository;

  @BeforeEach
  void clean() {
    Seed.clean(dataSource);
    Seed.exec(dataSource, "TRUNCATE chats.chats_user CASCADE");
  }

  @Test
  void findsOneToOneRoomByBothUsers() {
    UUID roomId = UUID.randomUUID();
    Seed.room(
        dataSource, roomId, "ONE_TO_ONE", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, true));

    assertThat(
            roomRepository.getOneToOneByAllUserIds(
                TestUsers.OWNER.toString(), TestUsers.MEMBER.toString()))
        .map(Room::getId)
        .contains(roomId.toString());
    assertThat(
            roomRepository.getOneToOneByAllUserIds(
                TestUsers.OWNER.toString(), TestUsers.OUTSIDER.toString()))
        .isEmpty();
  }

  @Test
  void loadsRoomWithSubscriptionsAndRoundTripsUpdates() {
    UUID roomId = UUID.randomUUID();
    Seed.room(dataSource, roomId, "GROUP", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, false));

    Room room = roomRepository.getById(roomId.toString()).orElseThrow();
    assertThat(room.getSubscriptions()).hasSize(2);
    assertThat(roomRepository.getByUserId(TestUsers.MEMBER.toString(), true))
        .extracting(Room::getId)
        .containsExactly(roomId.toString());

    roomRepository.update(room.name("renamed"));
    assertThat(roomRepository.getById(roomId.toString()).orElseThrow().getName())
        .isEqualTo("renamed");
  }

  @Test
  void savesAndFindsUsers() {
    userRepository.save(User.create().id("user-1").statusMessage("hi"));

    assertThat(userRepository.getById("user-1")).map(User::getStatusMessage).contains("hi");
    assertThat(userRepository.getById("missing")).isEmpty();
  }
}
