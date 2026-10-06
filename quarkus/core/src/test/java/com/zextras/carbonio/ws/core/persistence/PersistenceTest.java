// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PersistenceTest {

  @Inject DataSource ds;
  @Inject MeetingRepository meetings;
  @Inject ParticipantRepository participants;
  @Inject SubscriptionRepository subscriptions;
  @Inject VideoServerMeetingRepository videoServerMeetings;

  @BeforeEach
  void clean() {
    Seed.clean(ds);
  }

  @Test
  void coreMigrationsAreApplied() {
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.flyway_schema_history WHERE success AND type = 'SQL'"))
        .isEqualTo(12);
  }

  @Test
  void entitiesMapTheCeSchema() {
    UUID roomId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, false));
    Seed.meeting(ds, meetingId, roomId, true);
    Seed.participant(ds, meetingId, TestUsers.MEMBER);
    Seed.videoServerMeeting(ds, meetingId, "vs-1");

    QuarkusTransaction.requiringNew().run(() -> {
      MeetingEntity meeting = meetings.findById(meetingId.toString());
      assertThat(meeting.roomId).isEqualTo(roomId.toString());
      assertThat(meeting.active).isTrue();
      assertThat(meeting.startedAt).isNotNull();
      assertThat(participants.listByMeeting(meetingId.toString()))
          .extracting(p -> p.userId).containsExactly(TestUsers.MEMBER.toString());
      assertThat(subscriptions.find(roomId.toString(), TestUsers.OWNER.toString()))
          .get().extracting(s -> s.owner).isEqualTo(true);
      assertThat(subscriptions.listByRoom(roomId.toString())).hasSize(2);
      assertThat(videoServerMeetings.findById(meetingId.toString()).videoRoomId).isEqualTo("video-room");
    });
  }

  @Test
  void insertedMeetingGetsCreationTimestamp() {
    UUID roomId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true));
    String id = UUID.randomUUID().toString();

    QuarkusTransaction.requiringNew().run(() -> {
      MeetingEntity m = new MeetingEntity();
      m.id = id;
      m.roomId = roomId.toString();
      m.name = "m";
      m.meetingType = "PERMANENT";
      m.active = false;
      meetings.persist(m);
    });

    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.meeting WHERE id = ? AND created_at IS NOT NULL", id))
        .isEqualTo(1);
  }
}
