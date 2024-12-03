// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.core.data.entity.Recording;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingDeleted;
import com.zextras.carbonio.chats.core.data.event.MeetingRecordingStarted;
import com.zextras.carbonio.chats.core.data.event.MeetingRecordingStopped;
import com.zextras.carbonio.chats.core.data.event.MeetingStarted;
import com.zextras.carbonio.chats.core.data.event.MeetingStopped;
import com.zextras.carbonio.chats.core.data.model.RecordingInfo;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.data.type.RecordingStatus;
import com.zextras.carbonio.chats.core.exception.*;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.WaitingParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import com.zextras.carbonio.meeting.model.ParticipantDto;
import jakarta.ws.rs.core.Response.Status;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class MeetingServiceImplTest {

  private final MeetingService meetingService;
  private final MeetingRepository meetingRepository;
  private final RoomService roomService;
  private final MembersService membersService;
  private final WaitingParticipantService waitingParticipantService;
  private final VideoServerService videoServerService;
  private final VideoRecorderService videoRecorderService;
  private final EventDispatcher eventDispatcher;
  private final Clock clock;

  public MeetingServiceImplTest(MeetingMapper meetingMapper) {
    this.meetingRepository = mock(MeetingRepository.class);
    this.roomService = mock(RoomService.class);
    this.membersService = mock(MembersService.class);
    this.waitingParticipantService = mock(WaitingParticipantService.class);
    this.videoServerService = mock(VideoServerService.class);
    this.videoRecorderService = mock(VideoRecorderService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.clock = mock(Clock.class);
    this.meetingService =
        new MeetingServiceImpl(
            meetingRepository,
            meetingMapper,
            roomService,
            membersService,
            waitingParticipantService,
            videoServerService,
            videoRecorderService,
            eventDispatcher,
            clock);
  }

  private UUID user1Id;
  private UUID session1User1Id;
  private UUID user2Id;
  private UUID session1User2Id;
  private UUID user3Id;
  private UUID session1User3Id;

  private UUID room1Id;
  private UUID room2Id;
  private UUID room3Id;
  private Room room1;
  private Room room2;
  private UUID meeting1Id;
  private UUID meeting2Id;
  private Meeting meeting1;
  private Meeting meeting2;

  @BeforeEach
  public void init() {
    when(clock.instant()).thenReturn(Instant.parse("2022-01-01T11:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneId.of("UTC+01:00"));
    when(videoServerService.startMeeting(anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));
    when(videoServerService.stopMeeting(anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));
    when(videoServerService.startRecording(anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));
    when(videoServerService.stopRecording(anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));
    when(videoRecorderService.startRecordingPostProcessing(any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    user1Id = UUID.randomUUID();
    session1User1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    session1User2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    session1User3Id = UUID.randomUUID();

    room1Id = UUID.randomUUID();
    room1 = Room.create();
    room1
        .id(room1Id.toString())
        .type(RoomTypeDto.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(
            List.of(
                Subscription.create(room1, user1Id.toString()).owner(true),
                Subscription.create(room1, user2Id.toString()).owner(false),
                Subscription.create(room1, user3Id.toString()).owner(false)));
    room2Id = UUID.randomUUID();
    room3Id = UUID.randomUUID();

    meeting1Id = UUID.randomUUID();
    meeting1 = Meeting.create();
    meeting1
        .id(meeting1Id.toString())
        .roomId(room1Id.toString())
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
    room2 = Room.create();
    room2.id(room2Id.toString()).meetingId(meeting1Id.toString());
    meeting2Id = UUID.randomUUID();
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

  @Nested
  @DisplayName("Create Meeting tests")
  class CreateMeetingTests {

    @Test
    @DisplayName("Create meeting from room test")
    void createMeetingFromRoom_testOk() {
      UserPrincipal user = UserPrincipal.create(user1Id);
      String meetingName = "test";
      MeetingType meetingType = MeetingType.PERMANENT;
      UUID meetingId = UUID.randomUUID();
      Meeting meeting =
          Meeting.create()
              .id(meetingId.toString())
              .name(meetingName)
              .meetingType(meetingType)
              .roomId(room1Id.toString())
              .active(false);
      when(roomService.getRoomAndValidateUser(room1Id, user, false)).thenReturn(room1);
      when(meetingRepository.insert(any(Meeting.class))).thenReturn(meeting);

      MeetingDto createdMeeting =
          meetingService.createMeeting(user, meetingName, MeetingTypeDto.PERMANENT, room1Id, null);
      assertEquals(createdMeeting.getId(), meetingId);
      assertEquals(createdMeeting.getRoomId(), room1Id);
    }

    @Test
    @DisplayName("Fail to create meeting if already present")
    void createMeetingFromRoom_testKO() {
      UserPrincipal user = UserPrincipal.create(user1Id);
      String meetingName = "test";
      when(roomService.getRoomAndValidateUser(room2Id, user, false)).thenReturn(room2);

      assertThrows(
          ConflictException.class,
          () ->
              meetingService.createMeeting(
                  user, meetingName, MeetingTypeDto.PERMANENT, room2Id, null));
    }
  }

  @Nested
  @DisplayName("start meeting tests")
  class StartMeetingTests {

    @Test
    @DisplayName("Starts a meeting")
    void startMeeting_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID meetingId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      Meeting meeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .active(false);
      Meeting updatedMeeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .startedAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
              .active(true);
      when(meetingRepository.getById(meetingId.toString())).thenReturn(Optional.of(meeting));
      when(meetingRepository.update(updatedMeeting)).thenReturn(updatedMeeting);
      when(roomService.getRoomById(roomId, currentUser))
          .thenReturn(RoomDto.create().members(List.of(MemberDto.create().userId(user1Id))));
      meetingService.startMeeting(currentUser, meetingId);
      verify(meetingRepository, times(1)).getById(meetingId.toString());
      verify(meetingRepository, times(1)).update(updatedMeeting);
      verify(videoServerService, times(1)).startMeeting(meetingId.toString());
      verify(videoServerService, times(0)).stopMeeting(meetingId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()),
              MeetingStarted.create()
                  .meetingId(meetingId)
                  .starterUser(user1Id)
                  .startedAt(OffsetDateTime.parse("2022-01-01T13:00:00Z")));
      verifyNoMoreInteractions(videoServerService, videoRecorderService, meetingRepository);
    }

    @Test
    @DisplayName("Starts a meeting that does not exist")
    void startMeeting_testErrorMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> meetingService.startMeeting(UserPrincipal.create(user1Id), meeting1Id));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }
  }

  @Nested
  @DisplayName("Stop meeting tests")
  class StopMeetingTests {

    @Test
    @DisplayName("Stops a meeting")
    void stopMeeting_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID meetingId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      Meeting meeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .startedAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
              .active(true);
      Meeting updatedMeeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .active(false);
      when(meetingRepository.getById(meetingId.toString())).thenReturn(Optional.of(meeting));
      when(meetingRepository.update(updatedMeeting)).thenReturn(updatedMeeting);
      when(roomService.getRoomById(roomId, currentUser))
          .thenReturn(RoomDto.create().members(List.of(MemberDto.create().userId(user1Id))));
      meetingService.stopMeeting(currentUser, meetingId);
      verify(meetingRepository, times(1)).getById(meetingId.toString());
      verify(meetingRepository, times(1)).update(updatedMeeting);
      verify(videoServerService, times(0)).startMeeting(meetingId.toString());
      verify(videoServerService, times(1)).stopMeeting(meetingId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString()), MeetingStopped.create().meetingId(meetingId));
      verifyNoMoreInteractions(videoServerService, videoRecorderService, meetingRepository);
    }

    @Test
    @DisplayName("Stops a meeting with users in waiting list")
    void stopMeetingWithWaiting_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID meetingId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      Meeting meeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .active(true);
      Meeting updatedMeeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .active(false);
      when(waitingParticipantService.getQueue(meetingId)).thenReturn(List.of(user2Id));
      when(meetingRepository.getById(meetingId.toString())).thenReturn(Optional.of(meeting));
      when(meetingRepository.update(updatedMeeting)).thenReturn(updatedMeeting);
      when(roomService.getRoomById(roomId, currentUser))
          .thenReturn(RoomDto.create().members(List.of(MemberDto.create().userId(user1Id))));
      meetingService.stopMeeting(currentUser, meetingId);
      verify(meetingRepository, times(1)).getById(meetingId.toString());
      verify(meetingRepository, times(1)).update(updatedMeeting);
      verify(videoServerService, times(0)).startMeeting(meetingId.toString());
      verify(videoServerService, times(1)).stopMeeting(meetingId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString()),
              MeetingStopped.create().meetingId(meetingId));
      verifyNoMoreInteractions(videoServerService, videoRecorderService, meetingRepository);
    }

    @Test
    @DisplayName("Stops a meeting with recording ongoing")
    void stopMeetingWithRecording_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID meetingId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      Recording recording =
          Recording.create()
              .status(RecordingStatus.STARTED)
              .startedAt(OffsetDateTime.parse("2022-01-01T12:00+01:00"))
              .token("fake-token");
      Meeting meeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .active(true)
              .recordings(List.of(recording));
      Meeting updatedMeeting =
          Meeting.create()
              .roomId(roomId.toString())
              .name("test")
              .meetingType(MeetingType.PERMANENT)
              .id(meetingId.toString())
              .active(false);
      when(waitingParticipantService.getQueue(meetingId)).thenReturn(List.of(user2Id));
      when(meetingRepository.getById(meetingId.toString())).thenReturn(Optional.of(meeting));
      when(meetingRepository.update(updatedMeeting)).thenReturn(updatedMeeting);
      when(roomService.getRoomById(roomId, currentUser))
          .thenReturn(RoomDto.create().members(List.of(MemberDto.create().userId(user1Id))));
      meetingService.stopMeeting(currentUser, meetingId);
      verify(meetingRepository, times(1)).getById(meetingId.toString());
      verify(meetingRepository, times(1)).update(updatedMeeting);
      verify(videoServerService, times(0)).startMeeting(meetingId.toString());
      verify(videoServerService, times(1)).stopMeeting(meetingId.toString());
      verify(videoServerService, times(1)).stopRecording(meetingId.toString());
      verify(videoRecorderService, times(1))
          .startRecordingPostProcessing(
              RecordingInfo.create()
                  .meetingId(meetingId.toString())
                  .meetingName("test")
                  .recordingToken("fake-token"));
      verify(videoRecorderService, times(1)).saveRecordingStopped(recording);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString()),
              MeetingStopped.create().meetingId(meetingId));
      verifyNoMoreInteractions(videoServerService, videoRecorderService, meetingRepository);
    }

    @Test
    @DisplayName("Stops a meeting that does not exist")
    void stopMeeting_testErrorMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> meetingService.stopMeeting(UserPrincipal.create(user1Id), meeting1Id));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }
  }

  @Nested
  @DisplayName("List meetings tests")
  class ListMeetingTests {

    @Test
    @DisplayName("Returns all meetings of a user with all participants")
    void listMeeting_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      List<UUID> roomsIds = List.of(room1Id, room2Id, room3Id);
      when(roomService.getRoomsIds(currentUser)).thenReturn(roomsIds);
      when(meetingRepository.getByRoomsIds(
              List.of(room1Id.toString(), room2Id.toString(), room3Id.toString())))
          .thenReturn(List.of(meeting1, meeting2));

      List<MeetingDto> meetings = meetingService.getMeetings(currentUser);

      assertNotNull(meetings);
      assertEquals(2, meetings.size());
      MeetingDto meeting1Dto = meetings.get(0);
      assertEquals(meeting1Id, meeting1Dto.getId());
      assertEquals(room1Id, meeting1Dto.getRoomId());
      assertNotNull(meeting1Dto.getParticipants());
      assertEquals(3, meeting1Dto.getParticipants().size());
      assertEquals(
          1,
          meeting1Dto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .count());
      assertEquals(
          1,
          meeting1Dto.getParticipants().stream()
              .filter(p -> user2Id.equals(p.getUserId()))
              .count());
      assertEquals(
          1,
          meeting1Dto.getParticipants().stream()
              .filter(p -> user3Id.equals(p.getUserId()))
              .count());
      Optional<ParticipantDto> participant1 =
          meeting1Dto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertEquals(session1User1Id.toString(), participant1.get().getQueueId());
      assertTrue(participant1.get().isVideoStreamEnabled());
      assertTrue(participant1.get().isAudioStreamEnabled());

      MeetingDto meeting2Dto = meetings.get(1);
      assertEquals(meeting2Id, meeting2Dto.getId());
      assertEquals(room2Id, meeting2Dto.getRoomId());
      assertNotNull(meeting2Dto.getParticipants());
      assertEquals(2, meeting2Dto.getParticipants().size());
      assertEquals(
          1,
          meeting2Dto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .count());
      assertEquals(
          1,
          meeting2Dto.getParticipants().stream()
              .filter(p -> user3Id.equals(p.getUserId()))
              .count());
      participant1 =
          meeting2Dto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertEquals(session1User1Id.toString(), participant1.get().getQueueId());
      assertTrue(participant1.get().isVideoStreamEnabled());
      assertTrue(participant1.get().isAudioStreamEnabled());

      verify(roomService, times(1)).getRoomsIds(currentUser);
      verify(meetingRepository, times(1))
          .getByRoomsIds(List.of(room1Id.toString(), room2Id.toString(), room3Id.toString()));
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If rooms, which user is member of, hasn't any meetings, it returns an empty list")
    void listMeeting_testUserRoomsHasNoMeetings() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomsIds(currentUser)).thenReturn(List.of(room1Id, room2Id, room3Id));
      when(meetingRepository.getByRoomsIds(
              List.of(room1Id.toString(), room2Id.toString(), room3Id.toString())))
          .thenReturn(List.of());

      List<MeetingDto> meetings = meetingService.getMeetings(currentUser);

      assertNotNull(meetings);
      assertEquals(0, meetings.size());

      verify(roomService, times(1)).getRoomsIds(currentUser);
      verify(meetingRepository, times(1))
          .getByRoomsIds(List.of(room1Id.toString(), room2Id.toString(), room3Id.toString()));
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the user is not a member of any room, it returns an empty list")
    void listMeeting_testUserHasNotRooms() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);

      when(roomService.getRoomsIds(currentUser)).thenReturn(List.of());
      when(meetingRepository.getByRoomsIds(List.of())).thenReturn(List.of());

      List<MeetingDto> meetings = meetingService.getMeetings(currentUser);

      assertNotNull(meetings);
      assertEquals(0, meetings.size());
      verify(roomService, times(1)).getRoomsIds(currentUser);
      verify(meetingRepository, times(1)).getByRoomsIds(List.of());
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Get meeting by id tests")
  class GetMeetingByIdTests {

    @Test
    @DisplayName("Returns the required meeting with all participants")
    void getMeetingById_testOk() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id))
          .thenReturn(Optional.of(Subscription.create()));

      MeetingDto meetingDto =
          meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id));

      assertNotNull(meetingDto);
      assertEquals(meeting1Id, meetingDto.getId());
      assertEquals(room1Id, meetingDto.getRoomId());
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
          meetingDto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertEquals(session1User1Id.toString(), participant1.get().getQueueId());
      assertTrue(participant1.get().isVideoStreamEnabled());
      assertTrue(participant1.get().isAudioStreamEnabled());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't a room meeting member, it throws a 'forbidden' exception")
    void getMeetingById_testUserNotRoomMeetingMember() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' hasn't access to the meeting with id '%s'",
              user1Id, meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the meeting doesn't exists, it throws a 'not found' exception")
    void getMeetingById_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(roomService, membersService, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Get meeting entity tests")
  class GetMeetingEntityTests {

    @Test
    @DisplayName("Returns the required meeting entity with all participants")
    void getMeetingEntity_TestOk() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));

      Optional<Meeting> meeting = meetingService.getMeetingEntity(meeting1Id);
      assertTrue(meeting.isPresent());
      assertEquals(meeting1Id.toString(), meeting.get().getId());
      assertEquals(room1Id.toString(), meeting.get().getRoomId());
      assertNotNull(meeting.get().getParticipants());
      assertEquals(3, meeting.get().getParticipants().size());
      assertEquals(
          1,
          meeting.get().getParticipants().stream()
              .filter(p -> user1Id.toString().equals(p.getUserId()))
              .count());
      assertEquals(
          1,
          meeting.get().getParticipants().stream()
              .filter(p -> user2Id.toString().equals(p.getUserId()))
              .count());
      assertEquals(
          1,
          meeting.get().getParticipants().stream()
              .filter(p -> user3Id.toString().equals(p.getUserId()))
              .count());
      Optional<Participant> participant1 =
          meeting.get().getParticipants().stream()
              .filter(p -> user1Id.toString().equals(p.getUserId()))
              .findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id.toString(), participant1.get().getUserId());
      assertEquals(session1User1Id.toString(), participant1.get().getQueueId());
      assertTrue(participant1.get().hasVideoStreamOn());
      assertTrue(participant1.get().hasAudioStreamOn());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(membersService, roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the meeting doesn't exists, it throws a 'not found' exception")
    void getMeetingEntity_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(membersService, roomService, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Get meeting by room id tests")
  class GetMeetingByRoomIdTests {

    @Test
    @DisplayName("Returns the meeting of the required with all participants")
    void getMeetingByRoomId_testOk() {
      when(roomService.getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false))
          .thenReturn(room1);
      when(meetingRepository.getByRoomId(room1Id.toString())).thenReturn(Optional.of(meeting1));

      MeetingDto meetingDto =
          meetingService.getMeetingByRoomId(room1Id, UserPrincipal.create(user1Id));

      assertNotNull(meetingDto);
      assertEquals(meeting1Id, meetingDto.getId());
      assertEquals(room1Id, meetingDto.getRoomId());
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
          meetingDto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertEquals(session1User1Id.toString(), participant1.get().getQueueId());
      assertTrue(participant1.get().isVideoStreamEnabled());
      assertTrue(participant1.get().isAudioStreamEnabled());

      verify(roomService, times(1))
          .getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false);
      verify(meetingRepository, times(1)).getByRoomId(room1Id.toString());
      verifyNoMoreInteractions(meetingRepository, roomService);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the room meeting doesn't exists, it throws a 'not found' exception")
    void getMeetingByRoomId_testMeetingNotExists() {
      when(roomService.getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false))
          .thenReturn(room1);
      when(meetingRepository.getByRoomId(room1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> meetingService.getMeetingByRoomId(room1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting of the room with id '%s' doesn't exist", room1Id),
          exception.getMessage());

      verify(roomService, times(1))
          .getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false);
      verify(meetingRepository, times(1)).getByRoomId(room1Id.toString());
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Delete meeting by id tests")
  class DeleteMeetingByIdTests {

    @Test
    @DisplayName("Deletes the requested meeting")
    void deleteMeetingById_testOk() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(roomService.getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false))
          .thenReturn(room1);

      meetingService.deleteMeetingById(meeting1Id, UserPrincipal.create(user1Id));

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(meetingRepository, times(1)).delete(meeting1);
      verify(roomService, times(1))
          .getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false);
      verify(videoServerService, times(1)).stopMeeting(meeting1Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingDeleted.create().meetingId(meeting1Id));
      verifyNoMoreInteractions(meetingRepository, roomService, videoServerService, eventDispatcher);
      verifyNoInteractions(membersService);
    }

    @Test
    @DisplayName("Deletes the requested meeting which is being recorded")
    void deleteMeetingById_testOkWithRecording() {
      Recording recording =
          Recording.create()
              .status(RecordingStatus.STARTED)
              .startedAt(OffsetDateTime.parse("2022-01-01T12:00+01:00"))
              .token("fake-token");
      Meeting meeting = meeting1.recordings(List.of(recording));
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting));
      when(roomService.getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false))
          .thenReturn(room1);

      meetingService.deleteMeetingById(meeting1Id, UserPrincipal.create(user1Id));

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(meetingRepository, times(1)).delete(meeting);
      verify(roomService, times(1))
          .getRoomAndValidateUser(room1Id, UserPrincipal.create(user1Id), false);
      verify(videoServerService, times(1)).stopRecording(meeting1Id.toString());
      verify(videoRecorderService, times(1))
          .startRecordingPostProcessing(
              RecordingInfo.create()
                  .meetingId(meeting1Id.toString())
                  .meetingName(meeting.getName())
                  .recordingToken("fake-token"));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingRecordingStopped.create().meetingId(meeting1Id).userId(user1Id));
      verify(videoServerService, times(1)).stopMeeting(meeting1Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingDeleted.create().meetingId(meeting1Id));
      verifyNoMoreInteractions(meetingRepository, roomService, videoServerService, eventDispatcher);
      verifyNoInteractions(membersService);
    }

    @Test
    @DisplayName("If the meeting doesn't exist, it throws a 'not found' exception")
    void deleteMeetingById_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> meetingService.deleteMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(roomService, membersService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't a room meeting member, it throws a 'forbidden' exception")
    void getMeetingById_testUserNotRoomMeetingMember() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' hasn't access to the meeting with id '%s'",
              user1Id, meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the meeting doesn't exists, it throws a 'not found' exception")
    void getMeetingById_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(roomService, membersService, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Start meeting recording tests")
  class StartMeetingRecordingTests {

    @Test
    @DisplayName("Start recording on a meeting")
    void startMeetingRecording_testOk() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id))
          .thenReturn(
              Optional.of(
                  Subscription.create()
                      .userId(user1Id.toString())
                      .room(Room.create().id(room1Id.toString()))
                      .owner(true)));

      meetingService.startMeetingRecording(
          meeting1Id, UserPrincipal.create(user1Id).authToken("fake-token"));

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verify(videoServerService, times(1)).startRecording(meeting1Id.toString());

      verify(videoRecorderService, times(1))
          .saveRecordingStarted(meeting1, user1Id.toString(), "fake-token");

      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingRecordingStarted.create().userId(user1Id).meetingId(meeting1Id));
      verifyNoMoreInteractions(
          meetingRepository,
          membersService,
          videoServerService,
          videoRecorderService,
          eventDispatcher);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("Start recording on a meeting but it is already being recorded")
    void startMeetingRecording_testOkAlreadyStarted() {
      Meeting meeting =
          meeting1.recordings(
              List.of(
                  Recording.create()
                      .status(RecordingStatus.STARTED)
                      .startedAt(OffsetDateTime.parse("2022-01-01T12:00+01:00"))));
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting));
      when(membersService.getSubscription(user1Id, room1Id))
          .thenReturn(
              Optional.of(
                  Subscription.create()
                      .userId(user1Id.toString())
                      .room(Room.create().id(room1Id.toString()))
                      .owner(true)));

      meetingService.startMeetingRecording(meeting1Id, UserPrincipal.create(user1Id));

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Start recording on a meeting that does not exist")
    void startMeetingRecording_testErrorMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  meetingService.startMeetingRecording(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Start recording on a meeting not started")
    void startMeetingRecording_testErrorMeetingNotStarted() {
      Meeting meeting = meeting1.active(false);
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting));

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  meetingService.startMeetingRecording(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Can't start recording on this meeting", exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Start recording on a meeting but the user is not a participant")
    void startMeetingRecording_testErrorUserIsNotAParticipant() {
      Meeting meeting =
          meeting1.participants(
              List.of(
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
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting));

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  meetingService.startMeetingRecording(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Not Found - User '%s' not found into meeting '%s'", user1Id, meeting.getId()),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Start recording on a meeting but the user is not a room member")
    void startMeetingRecording_testErrorUserIsNotARoomMember() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  meetingService.startMeetingRecording(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - User with id '%s' not found", user1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Start recording on a meeting but the user is not a moderator")
    void startMeetingRecording_testErrorUserIsNotAModerator() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id))
          .thenReturn(
              Optional.of(
                  Subscription.create()
                      .userId(user1Id.toString())
                      .room(Room.create().id(room1Id.toString()))
                      .owner(false)));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  meetingService.startMeetingRecording(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' can't start recording on the meeting with id '%s'",
              user1Id, meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(videoServerService, videoRecorderService, eventDispatcher, roomService);
    }
  }

  @Nested
  @DisplayName("Stop meeting recording tests")
  class StopMeetingRecordingTests {

    @Test
    @DisplayName("Stop recording on a meeting")
    void stopMeetingRecording_testOk() {
      Recording recording =
          Recording.create()
              .status(RecordingStatus.STARTED)
              .startedAt(OffsetDateTime.parse("2022-01-01T12:00+01:00"))
              .token("rec-token");
      Meeting meeting = meeting1.recordings(List.of(recording));
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting));
      when(membersService.getSubscription(user1Id, room1Id))
          .thenReturn(
              Optional.of(
                  Subscription.create()
                      .userId(user1Id.toString())
                      .room(Room.create().id(room1Id.toString()))
                      .owner(true)));

      meetingService.stopMeetingRecording(
          meeting1Id,
          "rec-name",
          "rec-dir-id",
          UserPrincipal.create(user1Id).authToken("fake-token"));

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verify(videoServerService, times(1)).stopRecording(meeting1Id.toString());
      verify(videoRecorderService, times(1))
          .startRecordingPostProcessing(
              RecordingInfo.create()
                  .meetingId(meeting1Id.toString())
                  .meetingName(meeting.getName())
                  .recordingName("rec-name")
                  .folderId("rec-dir-id")
                  .recordingToken("rec-token"));
      verify(videoRecorderService, times(1)).saveRecordingStopped(recording);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              MeetingRecordingStopped.create().meetingId(meeting1Id).userId(user1Id));
      verifyNoMoreInteractions(
          meetingRepository,
          membersService,
          videoServerService,
          videoRecorderService,
          eventDispatcher);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("Stop recording on a meeting but it is already stopped")
    void stopMeetingRecording_testOkAlreadyStopped() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id))
          .thenReturn(
              Optional.of(
                  Subscription.create()
                      .userId(user1Id.toString())
                      .room(Room.create().id(room1Id.toString()))
                      .owner(true)));

      meetingService.stopMeetingRecording(
          meeting1Id,
          "rec-name",
          "rec-dir-id",
          UserPrincipal.create(user1Id).authToken("fake-token"));

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Stop recording on a meeting that does not exist")
    void stopMeetingRecording_testErrorMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  meetingService.stopMeetingRecording(
                      meeting1Id,
                      "rec-name",
                      "rec-dir-id",
                      UserPrincipal.create(user1Id).authToken("fake-token")));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Meeting with id '%s' not found", meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Stop recording on a meeting not started")
    void stopMeetingRecording_testErrorMeetingNotStarted() {
      Meeting meeting = meeting1.active(false);
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting));

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  meetingService.stopMeetingRecording(
                      meeting1Id,
                      "rec-name",
                      "rec-dir-id",
                      UserPrincipal.create(user1Id).authToken("fake-token")));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Can't stop recording on this meeting", exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Stop recording on a meeting but the user is not a participant")
    void stopMeetingRecording_testErrorUserIsNotAParticipant() {
      Meeting meeting =
          meeting1.participants(
              List.of(
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
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting));

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  meetingService.stopMeetingRecording(
                      meeting1Id,
                      "rec-name",
                      "rec-dir-id",
                      UserPrincipal.create(user1Id).authToken("fake-token")));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Not Found - User '%s' not found into meeting '%s'", user1Id, meeting.getId()),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(
          membersService, videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Stop recording on a meeting but the user is not a room member")
    void stopMeetingRecording_testErrorUserIsNotARoomMember() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  meetingService.stopMeetingRecording(
                      meeting1Id,
                      "rec-name",
                      "rec-dir-id",
                      UserPrincipal.create(user1Id).authToken("fake-token")));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - User with id '%s' not found", user1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(videoServerService, videoRecorderService, eventDispatcher, roomService);
    }

    @Test
    @DisplayName("Stop recording on a meeting but the user is not a moderator")
    void stopMeetingRecording_testErrorUserIsNotAModerator() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getSubscription(user1Id, room1Id))
          .thenReturn(
              Optional.of(
                  Subscription.create()
                      .userId(user1Id.toString())
                      .room(Room.create().id(room1Id.toString()))
                      .owner(false)));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  meetingService.stopMeetingRecording(
                      meeting1Id,
                      "rec-name",
                      "rec-dir-id",
                      UserPrincipal.create(user1Id).authToken("fake-token")));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' can't stop recording on the meeting with id '%s'",
              user1Id, meeting1Id),
          exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getSubscription(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(videoServerService, videoRecorderService, eventDispatcher, roomService);
    }
  }
}
