// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
@DisplayName("Updates move the time columns with the database clock")
class TimedWritesIT {

  private static final OffsetDateTime YESTERDAY =
      OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).truncatedTo(ChronoUnit.SECONDS);

  private final RoomService roomService;
  private final ParticipantRepository participantRepository;
  private final MeetingRepository meetingRepository;
  private final IntegrationTestUtils integrationTestUtils;
  private final MeetingTestUtils meetingTestUtils;
  private final Database database;

  public TimedWritesIT(
      RoomService roomService,
      ParticipantRepository participantRepository,
      MeetingRepository meetingRepository,
      IntegrationTestUtils integrationTestUtils,
      MeetingTestUtils meetingTestUtils,
      Database database) {
    this.roomService = roomService;
    this.participantRepository = participantRepository;
    this.meetingRepository = meetingRepository;
    this.integrationTestUtils = integrationTestUtils;
    this.meetingTestUtils = meetingTestUtils;
    this.database = database;
  }

  private OffsetDateTime time(String sql, Object... params) {
    var query = database.sqlQuery(sql);
    for (int i = 0; i < params.length; i++) {
      query.setParameter(i + 1, params[i]);
    }
    var timestamp = query.findOne().getTimestamp("t");
    return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
  }

  @Test
  @DisplayName("Renaming and re-describing a room moves updated_at and orders the two lines")
  void updateRoomMovesUpdatedAtAndOrdersLines() {
    UUID userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "before", List.of(userId, UUID.randomUUID()));
    integrationTestUtils.setTimes(
        "room", "id", roomId.toString(), YESTERDAY, "created_at", "updated_at");

    roomService.updateRoom(
        roomId,
        RoomEditableFieldsDto.create().name("after").description("new description"),
        UserPrincipal.create(userId));

    assertEquals(
        YESTERDAY.toInstant(),
        time("SELECT created_at AS t FROM chats.room WHERE id = ?", roomId.toString()).toInstant());
    assertTrue(
        time("SELECT updated_at AS t FROM chats.room WHERE id = ?", roomId.toString())
            .isAfter(YESTERDAY));
    OffsetDateTime nameLine =
        time(
            "SELECT created_at AS t FROM chats.system_event WHERE room_id = ? AND type ="
                + " 'ROOM_NAME_CHANGED'",
            roomId.toString());
    OffsetDateTime descriptionLine =
        time(
            "SELECT created_at AS t FROM chats.system_event WHERE room_id = ? AND type ="
                + " 'ROOM_DESCRIPTION_CHANGED'",
            roomId.toString());
    assertTrue(nameLine.isAfter(YESTERDAY));
    assertTrue(nameLine.isBefore(descriptionLine));
  }

  @Test
  @DisplayName("Muting then unmuting moves the settings updated_at and keeps created_at")
  void muteAndUnmuteMoveUpdatedAt() {
    UUID userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "room", List.of(userId, UUID.randomUUID()));
    UserPrincipal principal = UserPrincipal.create(userId);

    roomService.muteRoom(roomId, principal);
    String settings = " FROM chats.room_user_settings WHERE room_id = ? AND user_id = ?";
    assertNotNull(time("SELECT created_at AS t" + settings, roomId.toString(), userId.toString()));
    database
        .sqlUpdate(
            "UPDATE chats.room_user_settings SET created_at = :at, updated_at = :at"
                + " WHERE room_id = :roomId AND user_id = :userId")
        .setParameter("at", YESTERDAY)
        .setParameter("roomId", roomId.toString())
        .setParameter("userId", userId.toString())
        .execute();

    roomService.unmuteRoom(roomId, principal);

    assertEquals(
        YESTERDAY.toInstant(),
        time("SELECT created_at AS t" + settings, roomId.toString(), userId.toString())
            .toInstant());
    assertTrue(
        time("SELECT updated_at AS t" + settings, roomId.toString(), userId.toString())
            .isAfter(YESTERDAY));
  }

  @Test
  @DisplayName("Participant updates, hand raise and meeting start use the database clock")
  void participantAndMeetingTimes() {
    UUID userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(userId));
    UUID meetingId =
        meetingTestUtils.generateAndSaveMeeting(
            roomId, List.of(ParticipantBuilder.create(userId, UUID.randomUUID().toString())));
    String participantRow = " FROM chats.participant WHERE meeting_id = ? AND user_id = ?";
    database
        .sqlUpdate(
            "UPDATE chats.participant SET created_at = :at, updated_at = :at"
                + " WHERE meeting_id = :meetingId AND user_id = :userId")
        .setParameter("at", YESTERDAY)
        .setParameter("meetingId", meetingId.toString())
        .setParameter("userId", userId.toString())
        .execute();

    Participant participant =
        participantRepository.getById(meetingId.toString(), userId.toString()).orElseThrow();
    participantRepository.update(participant.audioStreamOn(true));

    assertEquals(
        YESTERDAY.toInstant(),
        time("SELECT created_at AS t" + participantRow, meetingId.toString(), userId.toString())
            .toInstant());
    OffsetDateTime afterMedia =
        time("SELECT updated_at AS t" + participantRow, meetingId.toString(), userId.toString());
    assertTrue(afterMedia.isAfter(YESTERDAY));
    assertTrue(
        participantRepository
            .getById(meetingId.toString(), userId.toString())
            .orElseThrow()
            .hasAudioStreamOn());

    participant = participantRepository.setHandRaised(participant, true);
    assertNotNull(participant.getHandRaisedAt());
    OffsetDateTime handRaisedAt =
        time(
            "SELECT hand_raised_at AS t" + participantRow, meetingId.toString(), userId.toString());
    assertEquals(participant.getHandRaisedAt().toInstant(), handRaisedAt.toInstant());
    assertTrue(handRaisedAt.isAfter(afterMedia));

    participant = participantRepository.setHandRaised(participant, false);
    assertNull(participant.getHandRaisedAt());
    assertNull(
        time(
            "SELECT hand_raised_at AS t" + participantRow,
            meetingId.toString(),
            userId.toString()));
    assertTrue(
        time("SELECT updated_at AS t" + participantRow, meetingId.toString(), userId.toString())
            .isAfter(handRaisedAt));

    Meeting meeting = meetingRepository.getById(meetingId.toString()).orElseThrow();
    assertNull(meeting.getStartedAt());
    meeting = meetingRepository.markStarted(meeting);
    assertTrue(meeting.getActive());
    assertEquals(
        meeting.getStartedAt().toInstant(),
        time("SELECT started_at AS t FROM chats.meeting WHERE id = ?", meetingId.toString())
            .toInstant());
  }
}
