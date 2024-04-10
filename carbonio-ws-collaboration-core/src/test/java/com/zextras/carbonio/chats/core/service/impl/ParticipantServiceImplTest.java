// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.*;
import com.zextras.carbonio.chats.core.data.event.*;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.WaitingParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.AudioStreamSettingsDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto.TypeEnum;
import com.zextras.carbonio.meeting.model.QueueUpdateStatusDto;
import jakarta.ws.rs.core.Response.Status;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class ParticipantServiceImplTest {

  private final ParticipantService participantService;
  private final MeetingService meetingService;
  private final RoomService roomService;
  private final MembersService membersService;
  private final ParticipantRepository participantRepository;
  private final WaitingParticipantRepository waitingParticipantRepository;
  private final VideoServerService videoServerService;
  private final EventDispatcher eventDispatcher;
  private final Clock clock;

  public ParticipantServiceImplTest() {
    this.meetingService = mock(MeetingService.class);
    this.roomService = mock(RoomService.class);
    this.membersService = mock(MembersService.class);
    this.participantRepository = mock(ParticipantRepository.class);
    this.waitingParticipantRepository = mock(WaitingParticipantRepository.class);
    this.videoServerService = mock(VideoServerService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.clock = mock(Clock.class);
    this.participantService =
        new ParticipantServiceImpl(
            meetingService,
            roomService,
            membersService,
            participantRepository,
            waitingParticipantRepository,
            videoServerService,
            eventDispatcher,
            clock);
  }

  private UUID user1Id;
  private UUID user1Queue1;
  private Participant participant1Session1;
  private UUID user2Id;
  private UUID user2Queue1;
  private Participant participant2Session1;
  private UUID user4Id;
  private UUID user4Queue1;
  private Participant participant4Session1;
  private UUID user3Id;
  private UUID user3Queue1;

  private UUID roomId;
  private Room room;

  private UUID scheduledRoomId;
  private Room scheduledRoom;

  private UUID permanentMeetingId;
  private Meeting permanentMeeting;
  private UUID meeting2Id;
  private Meeting meeting2;

  private UUID scheduledMeetingId;
  private Meeting scheduledMeeting;

  @BeforeEach
  public void init() {
    when(clock.instant()).thenReturn(Instant.parse("2022-01-01T11:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    user1Id = UUID.randomUUID();
    user1Queue1 = UUID.randomUUID();
    participant1Session1 =
        ParticipantBuilder.create(Meeting.create(), user1Id.toString())
            .queueId(user1Queue1)
            .audioStreamOn(false)
            .videoStreamOn(false)
            .screenStreamOn(false)
            .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
            .build();
    user2Id = UUID.randomUUID();
    user2Queue1 = UUID.randomUUID();
    participant2Session1 =
        ParticipantBuilder.create(Meeting.create(), user2Id.toString())
            .queueId(user2Queue1)
            .audioStreamOn(false)
            .videoStreamOn(false)
            .screenStreamOn(false)
            .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
            .build();
    user4Id = UUID.randomUUID();
    user4Queue1 = UUID.randomUUID();
    participant4Session1 =
        ParticipantBuilder.create(Meeting.create(), user4Id.toString())
            .queueId(user4Queue1)
            .audioStreamOn(true)
            .videoStreamOn(true)
            .screenStreamOn(true)
            .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
            .build();
    user3Id = UUID.randomUUID();
    user3Queue1 = UUID.randomUUID();

    roomId = UUID.randomUUID();
    room = Room.create();
    room.id(roomId.toString())
        .type(RoomTypeDto.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(
            List.of(
                Subscription.create(room, user1Id.toString()).owner(true),
                Subscription.create(room, user2Id.toString()),
                Subscription.create(room, user3Id.toString())));

    scheduledRoomId = UUID.randomUUID();
    scheduledRoom = Room.create();
    scheduledRoom
        .id(scheduledRoomId.toString())
        .type(RoomTypeDto.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(List.of(Subscription.create(scheduledRoom, user1Id.toString()).owner(true)));

    permanentMeetingId = UUID.randomUUID();
    permanentMeeting =
        MeetingBuilder.create(permanentMeetingId)
            .roomId(roomId)
            .createdAt(OffsetDateTime.parse("2022-01-01T12:00:00Z"))
            .meetingType(MeetingType.PERMANENT)
            .participants(List.of(participant1Session1, participant2Session1, participant4Session1))
            .build();
    meeting2Id = UUID.randomUUID();
    meeting2 =
        MeetingBuilder.create(meeting2Id)
            .roomId(roomId)
            .createdAt(OffsetDateTime.parse("2022-01-01T12:00:00Z"))
            .participants(List.of(participant2Session1))
            .build();

    scheduledMeetingId = UUID.randomUUID();
    scheduledMeeting =
        MeetingBuilder.create(scheduledMeetingId)
            .roomId(scheduledRoomId)
            .createdAt(OffsetDateTime.parse("2022-01-01T12:00:00Z"))
            .meetingType(MeetingType.SCHEDULED)
            .participants(List.of())
            .build();
  }

  @Nested
  @DisplayName("Insert PERMANENT meeting participant tests")
  class InsertMeetingParticipantTests {

    @Test
    @DisplayName("It inserts the current user as meeting participant")
    void insertMeetingParticipant_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).queueId(user3Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoom(roomId)).thenReturn(Optional.of(room));

      participantService.insertMeetingParticipant(
          permanentMeetingId,
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
          currentUser);

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(roomService, times(1)).getRoom(roomId);
      verify(participantRepository, times(1))
          .insert(
              Participant.create(permanentMeeting, user3Id.toString())
                  .queueId(user3Queue1.toString())
                  .createdAt(OffsetDateTime.now(clock)));
      verify(videoServerService, times(1))
          .addMeetingParticipant(
              user3Id.toString(),
              user3Queue1.toString(),
              permanentMeetingId.toString(),
              false,
              true);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingParticipantJoined.create().meetingId(permanentMeetingId).userId(user3Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("It kicks the previous session if the user was already inside the meeting")
    void insertMeetingParticipant_testOkRemovePreviousSessionBeforeJoining() {
      UUID newQueue = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(newQueue);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoom(roomId)).thenReturn(Optional.of(room));

      participantService.insertMeetingParticipant(
          permanentMeetingId,
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
          currentUser);

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);

      verify(roomService, times(1)).getRoom(roomId);
      verify(videoServerService, times(1))
          .destroyMeetingParticipant(user2Id.toString(), permanentMeetingId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingParticipantLeft.create().meetingId(permanentMeetingId).userId(user2Id));
      verify(eventDispatcher, times(1))
          .sendToUserQueue(
              user2Id.toString(),
              user2Queue1.toString(),
              MeetingParticipantClashed.create().meetingId(permanentMeetingId));
      verify(participantRepository, times(1))
          .update(
              participant2Session1
                  .audioStreamOn(false)
                  .videoStreamOn(false)
                  .screenStreamOn(false)
                  .queueId(newQueue.toString()));
      verify(videoServerService, times(1))
          .addMeetingParticipant(
              user2Id.toString(), newQueue.toString(), permanentMeetingId.toString(), false, true);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingParticipantJoined.create().meetingId(permanentMeetingId).userId(user2Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName(
        "If the current user is already a meeting participant, it throws a 'conflict' exception")
    void insertMeetingParticipant_testIsAlreadyMeetingParticipant() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoom(roomId)).thenReturn(Optional.of(room));
      ChatsHttpException exception =
          assertThrows(
              ConflictException.class,
              () ->
                  participantService.insertMeetingParticipant(
                      permanentMeetingId,
                      JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
                      currentUser));

      assertEquals(Status.CONFLICT.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.CONFLICT.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Conflict - User is already inserted into the meeting", exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(roomService, times(1)).getRoom(roomId);
      verifyNoMoreInteractions(roomService, meetingService);
      verifyNoInteractions(participantRepository, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Insert SCHEDULED meeting participant tests")
  class InsertScheduledMeetingParticipantTests {

    @Test
    @DisplayName("A moderator can directly enter the meeting")
    void insertMeetingParticipant_moderatorOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.ACCEPTED, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(participantRepository, times(1))
          .insert(
              Participant.create(scheduledMeeting, user1Id.toString())
                  .queueId(user1Queue1.toString())
                  .createdAt(OffsetDateTime.now(clock)));
      verify(videoServerService, times(1))
          .addMeetingParticipant(
              user1Id.toString(),
              user1Queue1.toString(),
              scheduledMeetingId.toString(),
              false,
              true);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()),
              MeetingParticipantJoined.create().meetingId(scheduledMeetingId).userId(user1Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("It kicks the previous session if the user was already inside the meeting")
    void insertMeetingParticipant_testOkRemovePreviousSessionBeforeJoining() {
      UUID newQueue = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(newQueue);
      scheduledMeeting.participants(List.of(participant1Session1));
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.ACCEPTED, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);

      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(videoServerService, times(1))
          .destroyMeetingParticipant(user1Id.toString(), scheduledMeetingId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()),
              MeetingParticipantLeft.create().meetingId(scheduledMeetingId).userId(user1Id));
      verify(eventDispatcher, times(1))
          .sendToUserQueue(
              user1Id.toString(),
              user1Queue1.toString(),
              MeetingParticipantClashed.create().meetingId(scheduledMeetingId));
      verify(participantRepository, times(1))
          .update(
              participant1Session1
                  .audioStreamOn(false)
                  .videoStreamOn(false)
                  .screenStreamOn(false)
                  .queueId(newQueue.toString()));
      verify(videoServerService, times(1))
          .addMeetingParticipant(
              user1Id.toString(), newQueue.toString(), scheduledMeetingId.toString(), false, true);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()),
              MeetingParticipantJoined.create().meetingId(scheduledMeetingId).userId(user1Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("A user is put on queue when trying to enter")
    void insertMeetingParticipant_userWaiting() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(user2Queue1);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.WAITING, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1))
          .insert(
              scheduledMeetingId.toString(),
              user2Id.toString(),
              user2Queue1.toString(),
              JoinStatus.WAITING);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()),
              MeetingWaitingParticipantJoined.create()
                  .meetingId(scheduledMeetingId)
                  .userId(user2Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("An accepted user can enter the meeting")
    void insertMeetingParticipant_userAccepted() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(user2Queue1);
      WaitingParticipant wp2 =
          new WaitingParticipant()
              .userId(user2Id.toString())
              .queueId(user2Queue1.toString())
              .status(JoinStatus.ACCEPTED);
      scheduledRoom.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString())));
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.find(
              scheduledMeetingId.toString(), user2Id.toString(), null))
          .thenReturn(List.of(wp2));

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.ACCEPTED, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(participantRepository, times(1))
          .insert(
              Participant.create(scheduledMeeting, user2Id.toString())
                  .queueId(user2Queue1.toString())
                  .createdAt(OffsetDateTime.now(clock)));
      verify(videoServerService, times(1))
          .addMeetingParticipant(
              user2Id.toString(),
              user2Queue1.toString(),
              scheduledMeetingId.toString(),
              false,
              true);
      verify(waitingParticipantRepository, times(1)).remove(wp2);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString()),
              MeetingParticipantJoined.create().meetingId(scheduledMeetingId).userId(user2Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("An user inside the room but still waiting is put on queue on the new device")
    void insertMeetingParticipant_userRoomWaitingClash() {
      UUID newQueue = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(newQueue);
      WaitingParticipant wp2 =
          new WaitingParticipant()
              .userId(user2Id.toString())
              .queueId(user2Queue1.toString())
              .status(JoinStatus.WAITING);
      scheduledRoom.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString())));
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.find(
              scheduledMeetingId.toString(), user2Id.toString(), null))
          .thenReturn(List.of(wp2));

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.WAITING, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1)).update(wp2.queueId(newQueue.toString()));
      verify(eventDispatcher, times(1))
          .sendToUserQueue(
              user2Id.toString(),
              user2Queue1.toString(),
              MeetingWaitingParticipantClashed.create().meetingId(scheduledMeetingId));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("An user inside the room but not on queue is put on waiting")
    void insertMeetingParticipant_userRoomNoQueue() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(user2Queue1);
      scheduledRoom.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString())));
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.find(
              scheduledMeetingId.toString(), user2Id.toString(), null))
          .thenReturn(Collections.emptyList());

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.WAITING, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1))
          .insert(
              scheduledMeetingId.toString(),
              user2Id.toString(),
              user2Queue1.toString(),
              JoinStatus.WAITING);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()),
              MeetingWaitingParticipantJoined.create()
                  .meetingId(scheduledMeetingId)
                  .userId(user2Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("An accepted user but not inside the room is put back on queue")
    void insertMeetingParticipant_userNoRoomAccepted() {
      UUID newQueue = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(newQueue);
      WaitingParticipant wp2 =
          new WaitingParticipant()
              .userId(user2Id.toString())
              .queueId(user2Queue1.toString())
              .status(JoinStatus.ACCEPTED);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.find(
              scheduledMeetingId.toString(), user2Id.toString(), null))
          .thenReturn(List.of(wp2));

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.WAITING, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1))
          .update(wp2.status(JoinStatus.WAITING).queueId(newQueue.toString()));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()),
              MeetingWaitingParticipantJoined.create()
                  .meetingId(scheduledMeetingId)
                  .userId(user2Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("A waiting user that rejoins is put on queue on the new device")
    void insertMeetingParticipant_userNoRoomWaitingRejoin() {
      UUID newQueue = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(newQueue);
      WaitingParticipant wp2 =
          new WaitingParticipant()
              .userId(user2Id.toString())
              .queueId(user2Queue1.toString())
              .status(JoinStatus.WAITING);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.find(
              scheduledMeetingId.toString(), user2Id.toString(), null))
          .thenReturn(List.of(wp2));

      JoinStatus meetingJoinStatus =
          participantService.insertMeetingParticipant(
              scheduledMeetingId,
              JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false),
              currentUser);

      assertEquals(JoinStatus.WAITING, meetingJoinStatus);
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1))
          .update(wp2.status(JoinStatus.WAITING).queueId(newQueue.toString()));
      verify(eventDispatcher, times(1))
          .sendToUserQueue(
              user2Id.toString(),
              user2Queue1.toString(),
              MeetingWaitingParticipantClashed.create().meetingId(scheduledMeetingId));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Remove meeting participant tests")
  class RemoveMeetingParticipantTests {

    @Test
    @DisplayName("It removes the current user as meeting participant")
    void removeMeetingParticipant_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user4Id).queueId(user4Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);
      when(participantRepository.getByMeetingId(permanentMeetingId.toString()))
          .thenReturn(List.of(participant2Session1));

      participantService.removeMeetingParticipant(permanentMeetingId, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1)).remove(participant4Session1);
      verify(participantRepository, times(1)).getByMeetingId(permanentMeetingId.toString());
      verify(videoServerService, times(1))
          .destroyMeetingParticipant(user4Id.toString(), permanentMeetingId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingParticipantLeft.create().meetingId(permanentMeetingId).userId(user4Id));
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName(
        "It removes the current user as meeting participant, "
            + "if it's the last one, the meeting is also stopped")
    void removeMeetingParticipant_testOkLastParticipant() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(user2Queue1);
      when(meetingService.getMeetingEntity(meeting2Id)).thenReturn(Optional.of(meeting2));
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);
      when(participantRepository.getByMeetingId(meeting2Id.toString())).thenReturn(List.of());

      participantService.removeMeetingParticipant(meeting2Id, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting2Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1)).remove(participant2Session1);
      verify(participantRepository, times(1)).getByMeetingId(meeting2Id.toString());
      verify(videoServerService, times(1))
          .destroyMeetingParticipant(user2Id.toString(), meeting2Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingParticipantLeft.create().meetingId(meeting2Id).userId(user2Id));
      verify(meetingService, times(1))
          .updateMeeting(UserPrincipal.create(user2Id), meeting2Id, false);
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the current user isn't a meeting participant, it ignores it silently")
    void removeMeetingParticipant_testIsNotMeetingParticipant() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).queueId(user3Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.removeMeetingParticipant(permanentMeetingId, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(meetingService, roomService);
      verifyNoInteractions(participantRepository, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Queue tests")
  class QueueTests {
    @Test
    @DisplayName("Return empty list if no user in queue")
    void getQueue_testEmpty() {
      when(waitingParticipantRepository.find(
              permanentMeetingId.toString(), null, JoinStatus.WAITING))
          .thenReturn(Collections.emptyList());
      List<UUID> queuedUsers = participantService.getQueue(permanentMeetingId);
      assertEquals(Collections.emptyList(), queuedUsers);
    }

    @Test
    @DisplayName("Return the list of users in queue")
    void getQueue_testOk() {
      when(waitingParticipantRepository.find(
              permanentMeetingId.toString(), null, JoinStatus.WAITING))
          .thenReturn(
              List.of(
                  new WaitingParticipant().userId(user1Id.toString()),
                  new WaitingParticipant().userId(user2Id.toString())));
      List<UUID> queuedUsers = participantService.getQueue(permanentMeetingId);
      assertEquals(2, queuedUsers.size());
      assertEquals(List.of(user1Id, user2Id), queuedUsers);
    }

    @Test
    @DisplayName("Reject a user from queue")
    void updateQueue_rejectUser() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoom(roomId)).thenReturn(Optional.of(room));
      when(waitingParticipantRepository.find(
              permanentMeetingId.toString(), user2Id.toString(), null))
          .thenReturn(
              List.of(
                  new WaitingParticipant()
                      .userId(user2Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user2Queue1.toString())));
      participantService.updateQueue(
          permanentMeetingId, user2Id, QueueUpdateStatusDto.REJECTED, currentUser);

      verify(waitingParticipantRepository, times(1))
          .remove(
              WaitingParticipant.create()
                  .userId(user2Id.toString())
                  .status(JoinStatus.WAITING)
                  .queueId(user2Queue1.toString()));
      DomainEvent event =
          MeetingWaitingParticipantRejected.create()
              .meetingId(permanentMeetingId)
              .userId(UUID.fromString(user2Id.toString()));
      verify(eventDispatcher).sendToUserExchange(List.of(user1Id.toString()), event);
      verify(eventDispatcher).sendToUserQueue(user2Id.toString(), user2Queue1.toString(), event);
    }

    @Test
    @DisplayName("Exit from the queue from the user")
    void updateQueue_exitQueueUser() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoom(roomId)).thenReturn(Optional.of(room));
      when(waitingParticipantRepository.find(
              permanentMeetingId.toString(), user1Id.toString(), null))
          .thenReturn(
              List.of(
                  new WaitingParticipant()
                      .userId(user1Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user1Queue1.toString())));
      participantService.updateQueue(
          permanentMeetingId, user1Id, QueueUpdateStatusDto.REJECTED, currentUser);

      verify(waitingParticipantRepository, times(1))
          .remove(
              WaitingParticipant.create()
                  .userId(user1Id.toString())
                  .status(JoinStatus.WAITING)
                  .queueId(user1Queue1.toString()));
      DomainEvent event =
          MeetingWaitingParticipantRejected.create()
              .meetingId(permanentMeetingId)
              .userId(UUID.fromString(user1Id.toString()));
      verify(eventDispatcher).sendToUserExchange(List.of(user1Id.toString()), event);
      verify(eventDispatcher).sendToUserQueue(user1Id.toString(), user1Queue1.toString(), event);
    }

    @Test
    @DisplayName("Accept a user from queue")
    void updateQueue_acceptUser() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoom(roomId)).thenReturn(Optional.of(room));
      when(waitingParticipantRepository.find(
              permanentMeetingId.toString(), user2Id.toString(), null))
          .thenReturn(
              List.of(
                  new WaitingParticipant()
                      .userId(user2Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user2Queue1.toString())));
      participantService.updateQueue(
          permanentMeetingId, user2Id, QueueUpdateStatusDto.ACCEPTED, currentUser);

      verify(waitingParticipantRepository, times(1))
          .update(
              WaitingParticipant.create()
                  .userId(user2Id.toString())
                  .status(JoinStatus.ACCEPTED)
                  .queueId(user2Queue1.toString()));
      verify(membersService, times(1))
          .insertRoomMember(
              roomId,
              MemberToInsertDto.create()
                  .userId(user2Id)
                  .historyCleared(false)
                  .external(false)
                  .owner(false)
                  .temporary(false),
              currentUser);
      DomainEvent event =
          MeetingWaitingParticipantAccepted.create()
              .meetingId(permanentMeetingId)
              .userId(UUID.fromString(user2Id.toString()));
      verify(eventDispatcher).sendToUserExchange(List.of(user1Id.toString()), event);
      verify(eventDispatcher).sendToUserQueue(user2Id.toString(), user2Queue1.toString(), event);
    }

    @Test
    @DisplayName("Block a non owner from moderating the queue")
    void updateQueue_testKOnotOwner() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(user2Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoom(roomId)).thenReturn(Optional.of(room));
      when(waitingParticipantRepository.find(
              permanentMeetingId.toString(), user3Id.toString(), null))
          .thenReturn(
              List.of(
                  new WaitingParticipant()
                      .userId(user3Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user3Queue1.toString())));
      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  participantService.updateQueue(
                      permanentMeetingId, user3Id, QueueUpdateStatusDto.REJECTED, currentUser));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
    }
  }

  @Nested
  @DisplayName("Enable video stream tests")
  class EnableVideoStreamTests {

    @Test
    @DisplayName("It enables the video stream for the current user")
    void enableVideoStream_testOkEnableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp"),
          UserPrincipal.create(user1Id).queueId(user1Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(participantRepository, times(1))
          .update(
              ParticipantBuilder.create(Meeting.create(), user1Id.toString())
                  .queueId(user1Queue1)
                  .videoStreamOn(true)
                  .screenStreamOn(false)
                  .audioStreamOn(false)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .updatedAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build());
      verify(videoServerService, times(1))
          .updateMediaStream(
              user1Id.toString(),
              permanentMeetingId.toString(),
              MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp"));

      verifyNoMoreInteractions(meetingService, participantRepository, videoServerService);
      verifyNoInteractions(eventDispatcher, roomService);
    }

    @Test
    @DisplayName("If video stream is already enabled for the current user, correctly it ignores")
    void enableVideoStream_testOkVideoStreamAlreadyEnabledWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp"),
          UserPrincipal.create(user4Id).queueId(user4Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    void enableVideoStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(permanentMeetingId)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateMediaStream(
                      permanentMeetingId,
                      MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp"),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting '%s' not found", permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Disable video stream tests")
  class DisableVideoStreamTests {

    @Test
    @DisplayName("It disables the video stream for the current user")
    void disableVideoStream_testOkDisableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false),
          UserPrincipal.create(user4Id).queueId(user4Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(participantRepository, times(1))
          .update(
              ParticipantBuilder.create(Meeting.create(), user4Id.toString())
                  .queueId(user4Queue1)
                  .videoStreamOn(false)
                  .audioStreamOn(true)
                  .screenStreamOn(true)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build());
      verify(videoServerService, times(1))
          .updateMediaStream(
              user4Id.toString(),
              permanentMeetingId.toString(),
              MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user4Id.toString()),
              MeetingMediaStreamChanged.create()
                  .meetingId(permanentMeetingId)
                  .userId(user4Id)
                  .mediaType(MediaType.VIDEO)
                  .active(false));
      verifyNoMoreInteractions(
          meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName(
        "If video stream is already disabled for the current session, correctly it ignores")
    void disableVideoStream_testOkVideoStreamAlreadyDisabledWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false),
          UserPrincipal.create(user1Id).queueId(user1Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName(
        "If the requested user isn't in the meeting participants, it throws a 'not found'"
            + " exception")
    void disableVideoStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateMediaStream(
                      permanentMeetingId,
                      MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false),
                      UserPrincipal.create(user3Id).queueId(user3Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Not Found - User '%s' not found into meeting '%s'", user3Id, permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    void disableVideoStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(permanentMeetingId)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateMediaStream(
                      permanentMeetingId,
                      MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting '%s' not found", permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Enable audio stream tests")
  public class EnableAudioStreamTests {

    private final boolean hasAudioStreamOn = true;

    @Test
    @DisplayName("It enables the audio stream for the current session")
    void enableAudioStream_testOkEnableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateAudioStream(
          permanentMeetingId,
          AudioStreamSettingsDto.create().enabled(hasAudioStreamOn),
          UserPrincipal.create(user1Id).queueId(user1Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(participantRepository, times(1))
          .update(
              ParticipantBuilder.create(Meeting.create(), user1Id.toString())
                  .queueId(user1Queue1)
                  .audioStreamOn(hasAudioStreamOn)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user4Id.toString()),
              MeetingAudioStreamChanged.create()
                  .meetingId(permanentMeetingId)
                  .userId(user1Id)
                  .active(true));
      verify(videoServerService, times(1))
          .updateAudioStream(user1Id.toString(), permanentMeetingId.toString(), true);

      verifyNoMoreInteractions(
          meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("If audio stream is already enabled for the current session, correctly it ignores")
    void enableAudioStream_testOkAudioStreamAlreadyEnabledWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateAudioStream(
          permanentMeetingId,
          AudioStreamSettingsDto.create().enabled(hasAudioStreamOn),
          UserPrincipal.create(user4Id).queueId(user4Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName(
        "If the current session is not the requested session, it throws a 'bad request' exception")
    void enableAudioStream_testErrorEnableWithSessionDifferentToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  participantService.updateAudioStream(
                      permanentMeetingId,
                      AudioStreamSettingsDto.create()
                          .userToModerate(user2Id.toString())
                          .enabled(hasAudioStreamOn),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Bad Request - User '%s' cannot enable the audio stream of the user '%s'",
              user1Id, user2Id),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName(
        "If the requested session isn't in the meeting participants, it throws a 'not found'"
            + " exception")
    void enableAudioStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateAudioStream(
                      permanentMeetingId,
                      AudioStreamSettingsDto.create()
                          .userToModerate(user3Id.toString())
                          .enabled(hasAudioStreamOn),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Not Found - User '%s' not found into meeting '%s'", user3Id, permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    void enableAudioStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(permanentMeetingId)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateAudioStream(
                      permanentMeetingId,
                      AudioStreamSettingsDto.create()
                          .userToModerate(user3Id.toString())
                          .enabled(hasAudioStreamOn),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting '%s' not found", permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Disable audio stream tests")
  class DisableAudioStreamTests {

    private final boolean hasAudioStreamOn = false;

    @Test
    @DisplayName("It disables the audio stream for the current user")
    void disableAudioStream_testOkDisableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateAudioStream(
          permanentMeetingId,
          AudioStreamSettingsDto.create().enabled(hasAudioStreamOn),
          UserPrincipal.create(user4Id).queueId(user4Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(participantRepository, times(1))
          .update(
              ParticipantBuilder.create(Meeting.create(), user4Id.toString())
                  .queueId(user4Queue1)
                  .audioStreamOn(hasAudioStreamOn)
                  .videoStreamOn(true)
                  .screenStreamOn(true)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user4Id.toString()),
              MeetingAudioStreamChanged.create().meetingId(permanentMeetingId).userId(user4Id));
      verify(videoServerService, times(1))
          .updateAudioStream(user4Id.toString(), permanentMeetingId.toString(), false);
      verifyNoMoreInteractions(
          meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("If audio stream is already disabled for the current user, correctly it ignores")
    void disableAudioStream_testOkAudioStreamAlreadyDisabledWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateAudioStream(
          permanentMeetingId,
          AudioStreamSettingsDto.create().enabled(hasAudioStreamOn),
          UserPrincipal.create(user1Id).queueId(user1Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("It disables the audio stream for another user")
    void disableAudioStream_testOkDisableWithAnotherSession() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateAudioStream(
          permanentMeetingId,
          AudioStreamSettingsDto.create()
              .userToModerate(user4Id.toString())
              .enabled(hasAudioStreamOn),
          currentUser);

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, true);
      verify(participantRepository, times(1))
          .update(
              ParticipantBuilder.create(Meeting.create(), user4Id.toString())
                  .queueId(user4Queue1)
                  .audioStreamOn(false)
                  .videoStreamOn(true)
                  .screenStreamOn(true)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user4Id.toString()),
              MeetingAudioStreamChanged.create()
                  .meetingId(permanentMeetingId)
                  .moderatorId(user1Id)
                  .userId(user4Id));
      verify(videoServerService, times(1))
          .updateAudioStream(user4Id.toString(), permanentMeetingId.toString(), false);
      verifyNoMoreInteractions(
          meetingService, roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If audio stream is already disabled for another user, correctly it ignores")
    void disableAudioStream_testOkAudioStreamAlreadyDisabledWithAnotherSession() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateAudioStream(
          permanentMeetingId,
          AudioStreamSettingsDto.create()
              .userToModerate(user2Id.toString())
              .enabled(hasAudioStreamOn),
          currentUser);

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, true);
      verifyNoMoreInteractions(meetingService, roomService);
      verifyNoInteractions(participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName(
        "If the requested user isn't in the meeting participants, it throws a 'not found'"
            + " exception")
    void disableAudioStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateAudioStream(
                      permanentMeetingId,
                      AudioStreamSettingsDto.create()
                          .userToModerate(user3Id.toString())
                          .enabled(hasAudioStreamOn),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Not Found - User '%s' not found into meeting '%s'", user3Id, permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    void disableAudioStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(permanentMeetingId)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateAudioStream(
                      permanentMeetingId,
                      AudioStreamSettingsDto.create()
                          .userToModerate(user3Id.toString())
                          .enabled(hasAudioStreamOn),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting '%s' not found", permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName(
        "If the user who performed the action isn't a moderator, it throws a 'forbidden' exception")
    void disableAudioStream_testErrorUserIsNotAModerator() {
      UserPrincipal user = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));
      when(roomService.getRoomEntityAndCheckUser(roomId, user, true))
          .thenThrow(
              new ForbiddenException(
                  String.format("User '%s' is not an owner of room '%s'", user.getId(), roomId)));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  participantService.updateAudioStream(
                      permanentMeetingId,
                      AudioStreamSettingsDto.create()
                          .userToModerate(user4Id.toString())
                          .enabled(hasAudioStreamOn),
                      user));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Forbidden - User '%s' is not an owner of room '%s'", user.getId(), roomId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, user, true);
      verifyNoInteractions(participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Enable screen stream tests")
  class EnableScreenStreamTests {

    @Test
    @DisplayName("It enables the screen stream for the current user")
    void enableScreenStream_testOkEnableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp"),
          UserPrincipal.create(user1Id).queueId(user1Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(participantRepository, times(1))
          .update(
              ParticipantBuilder.create(Meeting.create(), user1Id.toString())
                  .queueId(user1Queue1)
                  .screenStreamOn(true)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build());
      verify(videoServerService, times(1))
          .updateMediaStream(
              user1Id.toString(),
              permanentMeetingId.toString(),
              MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp"));

      verifyNoMoreInteractions(meetingService, participantRepository, videoServerService);
      verifyNoInteractions(eventDispatcher, roomService);
    }

    @Test
    @DisplayName("If screen stream is already enabled for the current user, correctly it ignores")
    void enableScreenStream_testOkScreenStreamAlreadyEnabledWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp"),
          UserPrincipal.create(user4Id).queueId(user4Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName(
        "If the requested user isn't in the meeting participants, it throws a 'not found'"
            + " exception")
    void enableScreenStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateMediaStream(
                      permanentMeetingId,
                      MediaStreamSettingsDto.create()
                          .type(TypeEnum.SCREEN)
                          .enabled(true)
                          .sdp("sdp"),
                      UserPrincipal.create(user3Id).queueId(user3Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Not Found - User '%s' not found into meeting '%s'", user3Id, permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    void enableScreenStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(permanentMeetingId)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateMediaStream(
                      permanentMeetingId,
                      MediaStreamSettingsDto.create()
                          .type(TypeEnum.SCREEN)
                          .enabled(true)
                          .sdp("sdp"),
                      UserPrincipal.create(user3Id).queueId(user3Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting '%s' not found", permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Disable screen stream tests")
  class DisableScreenStreamTests {

    @Test
    @DisplayName("It disables the screen stream for the current user")
    void disableScreenStream_testOkDisableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false),
          UserPrincipal.create(user4Id).queueId(user4Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verify(participantRepository, times(1))
          .update(
              ParticipantBuilder.create(Meeting.create(), user4Id.toString())
                  .queueId(user4Queue1)
                  .screenStreamOn(false)
                  .audioStreamOn(true)
                  .videoStreamOn(true)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build());
      verify(videoServerService, times(1))
          .updateMediaStream(
              user4Id.toString(),
              permanentMeetingId.toString(),
              MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user4Id.toString()),
              MeetingMediaStreamChanged.create()
                  .meetingId(permanentMeetingId)
                  .userId(user4Id)
                  .mediaType(MediaType.SCREEN)
                  .active(false));
      verifyNoMoreInteractions(
          meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("If screen stream is already disabled for the current user, correctly it ignores")
    void disableScreenStream_testOkScreenStreamAlreadyDisabledWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      participantService.updateMediaStream(
          permanentMeetingId,
          MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false),
          UserPrincipal.create(user1Id).queueId(user1Queue1));

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName(
        "If the requested user isn't in the meeting participants, it throws a 'not found'"
            + " exception")
    void disableScreenStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(permanentMeetingId))
          .thenReturn(Optional.of(permanentMeeting));

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateMediaStream(
                      permanentMeetingId,
                      MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false),
                      UserPrincipal.create(user3Id).queueId(user3Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Not Found - User '%s' not found into meeting '%s'", user3Id, permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    void disableScreenStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(permanentMeetingId)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  participantService.updateMediaStream(
                      permanentMeetingId,
                      MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false),
                      UserPrincipal.create(user1Id).queueId(user1Queue1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting '%s' not found", permanentMeetingId),
          exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(permanentMeetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }
}
