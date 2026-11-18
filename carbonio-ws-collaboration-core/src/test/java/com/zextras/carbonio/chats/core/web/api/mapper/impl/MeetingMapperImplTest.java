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
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.web.api.mapper.MeetingMapper;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import com.zextras.carbonio.chats.model.ParticipantDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class MeetingMapperImplTest {

  private final MeetingMapper meetingMapper;

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private UUID session1User1Id;
  private UUID session1User2Id;
  private UUID session1User3Id;
  private UUID room1Id;
  private UUID room2Id;
  private UUID meeting1Id;
  private UUID meeting2Id;
  private Meeting meeting1;
  private Meeting meeting2;

  MeetingMapperImplTest(MeetingMapper meetingMapper) {
    this.meetingMapper = meetingMapper;
  }

  @BeforeEach
  void init() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    session1User1Id = UUID.randomUUID();
    session1User2Id = UUID.randomUUID();
    session1User3Id = UUID.randomUUID();
    room1Id = UUID.randomUUID();
    room2Id = UUID.randomUUID();
    meeting1Id = UUID.randomUUID();
    meeting2Id = UUID.randomUUID();
    meeting1 = Meeting.create();
    meeting1
        .id(meeting1Id.toString())
        .roomId(room1Id.toString())
        .name("test")
        .meetingType(MeetingType.PERMANENT)
        .participants(
            List.of(
                ParticipantBuilder.create(meeting1, user1Id.toString())
                    .queueId(session1User1Id)
                    .audioStreamOn(true)
                    .videoStreamOn(true)
                    .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                    .build(),
                ParticipantBuilder.create(meeting1, user2Id.toString())
                    .queueId(session1User2Id)
                    .audioStreamOn(false)
                    .videoStreamOn(true)
                    .createdAt(OffsetDateTime.parse("2022-01-01T13:30:00Z"))
                    .build(),
                ParticipantBuilder.create(meeting1, user3Id.toString())
                    .queueId(session1User3Id)
                    .createdAt(OffsetDateTime.parse("2022-01-01T13:15:00Z"))
                    .build()));
    meeting2 = Meeting.create();
    meeting2
        .id(meeting2Id.toString())
        .roomId(room2Id.toString())
        .meetingType(MeetingType.SCHEDULED)
        .participants(
            List.of(
                ParticipantBuilder.create(meeting1, user1Id.toString())
                    .queueId(session1User1Id)
                    .audioStreamOn(true)
                    .videoStreamOn(true)
                    .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                    .build(),
                ParticipantBuilder.create(meeting1, user3Id.toString())
                    .queueId(session1User3Id)
                    .createdAt(OffsetDateTime.parse("2022-01-01T13:15:00Z"))
                    .build()));
  }

  private void assertMeeting1(MeetingDto meetingDto) {
    assertNotNull(meetingDto);
    assertEquals(meeting1Id, meetingDto.getId());
    assertEquals(room1Id, meetingDto.getRoomId());
    assertEquals(MeetingTypeDto.PERMANENT, meetingDto.getMeetingType());
    assertNotNull(meetingDto.getParticipants());
    assertEquals(3, meetingDto.getParticipants().size());
    assertEquals(
        1,
        meetingDto.getParticipants().stream().filter(p -> user1Id.equals(p.getUserId())).count());
    assertEquals(
        1,
        meetingDto.getParticipants().stream().filter(p -> user2Id.equals(p.getUserId())).count());
    assertEquals(
        1,
        meetingDto.getParticipants().stream().filter(p -> user3Id.equals(p.getUserId())).count());
    Optional<ParticipantDto> participant1 =
        meetingDto.getParticipants().stream().filter(p -> user1Id.equals(p.getUserId())).findAny();
    assertTrue(participant1.isPresent());
    assertEquals(user1Id, participant1.get().getUserId());
    assertEquals(session1User1Id.toString(), participant1.get().getQueueId());
    assertTrue(participant1.get().isVideoStreamEnabled());
    assertTrue(participant1.get().isAudioStreamEnabled());
    assertFalse(participant1.get().isScreenStreamEnabled());
    assertEquals(OffsetDateTime.parse("2022-01-01T13:00:00Z"), participant1.get().getJoinedAt());
    Optional<ParticipantDto> participant2 =
        meetingDto.getParticipants().stream().filter(p -> user2Id.equals(p.getUserId())).findAny();
    assertTrue(participant2.isPresent());
    assertFalse(participant2.get().isAudioStreamEnabled());
    assertTrue(participant2.get().isVideoStreamEnabled());
  }

  @Test
  @DisplayName("A meeting maps its fields and all its participants")
  void ent2dto_meeting() {
    assertMeeting1(meetingMapper.ent2dto(meeting1));
  }

  @Test
  @DisplayName("A list of meetings maps each meeting with its participants, keeping the order")
  void ent2dto_meetings() {
    List<MeetingDto> meetings = meetingMapper.ent2dto(List.of(meeting1, meeting2));

    assertNotNull(meetings);
    assertEquals(2, meetings.size());
    assertMeeting1(meetings.get(0));
    MeetingDto meeting2Dto = meetings.get(1);
    assertEquals(meeting2Id, meeting2Dto.getId());
    assertEquals(room2Id, meeting2Dto.getRoomId());
    assertEquals(MeetingTypeDto.SCHEDULED, meeting2Dto.getMeetingType());
    assertNotNull(meeting2Dto.getParticipants());
    assertEquals(2, meeting2Dto.getParticipants().size());
    assertEquals(
        1,
        meeting2Dto.getParticipants().stream().filter(p -> user1Id.equals(p.getUserId())).count());
    assertEquals(
        1,
        meeting2Dto.getParticipants().stream().filter(p -> user3Id.equals(p.getUserId())).count());
  }

  @Test
  @DisplayName("An empty or missing list maps to an empty list and a missing meeting to null")
  void ent2dto_emptyAndNull() {
    assertEquals(List.of(), meetingMapper.ent2dto(List.<Meeting>of()));
    assertEquals(List.of(), meetingMapper.ent2dto((List<Meeting>) null));
    assertNull(meetingMapper.ent2dto((Meeting) null));
  }
}
