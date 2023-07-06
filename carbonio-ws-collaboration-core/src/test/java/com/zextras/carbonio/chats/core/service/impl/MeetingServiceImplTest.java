// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingCreatedEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingDeletedEvent;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingUserDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import com.zextras.carbonio.meeting.model.ParticipantDto;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@UnitTest
public class MeetingServiceImplTest {

  private final MeetingService     meetingService;
  private final MeetingRepository  meetingRepository;
  private final RoomService        roomService;
  private final MembersService     membersService;
  private final VideoServerService videoServerService;
  private final EventDispatcher    eventDispatcher;

  public MeetingServiceImplTest(MeetingMapper meetingMapper) {
    this.meetingRepository = mock(MeetingRepository.class);
    this.roomService = mock(RoomService.class);
    this.membersService = mock(MembersService.class);
    this.videoServerService = mock(VideoServerService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.meetingService = new MeetingServiceImpl(
      this.meetingRepository,
      meetingMapper,
      this.roomService,
      this.membersService,
      this.videoServerService,
      this.eventDispatcher);
  }

  private UUID   user1Id;
  private String session1User1Id;
  private UUID   user2Id;
  private String session1User2Id;
  private String session2User2Id;
  private UUID   user3Id;
  private String session1User3Id;

  private UUID room1Id;
  private UUID room2Id;
  private UUID room3Id;
  private Room room1;

  private UUID    meeting1Id;
  private UUID    meeting2Id;
  private Meeting meeting1;
  private Meeting meeting2;

  @BeforeEach
  public void init() {
    user1Id = UUID.randomUUID();
    session1User1Id = "session1User1Id";
    user2Id = UUID.randomUUID();
    session1User2Id = "session1User2Id";
    session2User2Id = "session2User2Id";
    user3Id = UUID.randomUUID();
    session1User3Id = "session3User2Id";

    room1Id = UUID.randomUUID();
    room1 = Room.create();
    room1.id(room1Id.toString())
      .type(RoomTypeDto.GROUP)
      .name("room1")
      .description("Room one")
      .subscriptions(List.of(
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
      .participants(List.of(
        ParticipantBuilder.create(meeting1, session1User1Id).userId(user1Id).audioStreamOn(true).videoStreamOn(true)
          .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z")).build(),
        ParticipantBuilder.create(meeting1, session1User2Id).userId(user2Id).audioStreamOn(false).videoStreamOn(true)
          .createdAt(OffsetDateTime.parse("2022-01-01T13:30:00Z")).build(),
        ParticipantBuilder.create(meeting1, session2User2Id).userId(user2Id).audioStreamOn(true).videoStreamOn(false)
          .createdAt(OffsetDateTime.parse("2022-01-01T13:32:00Z")).build(),
        ParticipantBuilder.create(meeting1, session1User3Id).userId(user3Id).createdAt(
          OffsetDateTime.parse("2022-01-01T13:15:00Z")).build()));
    meeting2Id = UUID.randomUUID();
    meeting2 = Meeting.create();
    meeting2
      .id(meeting2Id.toString())
      .roomId(room2Id.toString())
      .participants(List.of(
        ParticipantBuilder.create(meeting1, session1User1Id).userId(user1Id).audioStreamOn(true).videoStreamOn(true)
          .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z")).build(),
        ParticipantBuilder.create(meeting1, session1User3Id).userId(user3Id).createdAt(
          OffsetDateTime.parse("2022-01-01T13:15:00Z")).build()));
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
      Meeting meeting = Meeting.create()
        .roomId(room1Id.toString())
        .meetingType(meetingType)
        .active(false)
        .id(meetingId.toString());
      when(roomService.getRoomEntityAndCheckUser(room1Id, user, false))
        .thenReturn(room1);
      when(meetingRepository.insert(meetingName, meetingType, room1Id, null)).thenReturn(meeting);

      MeetingDto createdMeeting = meetingService.createMeeting(
        user,
        meetingName,
        MeetingTypeDto.PERMANENT,
        room1Id,
        null,
        null
      );
      assertEquals(createdMeeting.getId(), meetingId);
      assertEquals(createdMeeting.getRoomId(), room1Id);
    }


    @Test
    @DisplayName("Create meeting from users")
    void createMeetingFromUsers_testOk() {
      UserPrincipal user = UserPrincipal.create(user1Id);
      String meetingName = "test";
      MeetingType meetingType = MeetingType.SCHEDULED;
      UUID meetingId = UUID.randomUUID();
      UUID user1Id = UUID.randomUUID();
      UUID user2Id = UUID.randomUUID();
      UUID newRoomId = UUID.randomUUID();
      List<MeetingUserDto> users = List.of(
        MeetingUserDto.create().userId(user1Id),
        MeetingUserDto.create().userId(user2Id)
      );
      Meeting meeting = Meeting.create()
        .roomId(newRoomId.toString())
        .meetingType(meetingType)
        .active(false)
        .id(meetingId.toString());
      when(roomService.createRoom(RoomCreationFieldsDto
        .create().name(meetingName).type(RoomTypeDto.GROUP).membersIds(List.of(user1Id,user2Id)),user))
        .thenReturn(RoomDto.create().id(newRoomId).name(meetingName));
      when(meetingRepository.insert(meetingName, meetingType, newRoomId, null)).thenReturn(meeting);

      MeetingDto createdMeeting = meetingService.createMeeting(
        user,
        meetingName,
        MeetingTypeDto.SCHEDULED,
        null,
        users,
        null
      );
      assertEquals(createdMeeting.getId(), meetingId);
      assertEquals(createdMeeting.getRoomId(), newRoomId);
    }
  }

  @Nested
  @DisplayName("List meetings tests")
  public class ListMeetingTests {

    @Test
    @DisplayName("Returns all meetings of a user with all participants")
    public void listMeeting_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      List<UUID> roomsIds = List.of(room1Id, room2Id, room3Id);
      when(roomService.getRoomsIds(currentUser)).thenReturn(roomsIds);
      when(meetingRepository.getByRoomsIds(List.of(room1Id.toString(), room2Id.toString(), room3Id.toString())))
        .thenReturn(List.of(meeting1, meeting2));

      List<MeetingDto> meetings = meetingService.getMeetings(currentUser);

      assertNotNull(meetings);
      assertEquals(2, meetings.size());
      MeetingDto meeting1Dto = meetings.get(0);
      assertEquals(meeting1Id, meeting1Dto.getId());
      assertEquals(room1Id, meeting1Dto.getRoomId());
      assertNotNull(meeting1Dto.getParticipants());
      assertEquals(4, meeting1Dto.getParticipants().size());
      assertEquals(1, (int) meeting1Dto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).count());
      assertEquals(2, (int) meeting1Dto.getParticipants().stream()
        .filter(p -> user2Id.equals(p.getUserId())).count());
      assertEquals(1, (int) meeting1Dto.getParticipants().stream()
        .filter(p -> user3Id.equals(p.getUserId())).count());
      Optional<ParticipantDto> participant1 = meeting1Dto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertTrue(participant1.get().isVideoStreamOn());
      assertTrue(participant1.get().isAudioStreamOn());

      MeetingDto meeting2Dto = meetings.get(1);
      assertEquals(meeting2Id, meeting2Dto.getId());
      assertEquals(room2Id, meeting2Dto.getRoomId());
      assertNotNull(meeting2Dto.getParticipants());
      assertEquals(2, meeting2Dto.getParticipants().size());
      assertEquals(1, (int) meeting2Dto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).count());
      assertEquals(1, (int) meeting2Dto.getParticipants().stream()
        .filter(p -> user3Id.equals(p.getUserId())).count());
      participant1 = meeting2Dto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertTrue(participant1.get().isVideoStreamOn());
      assertTrue(participant1.get().isAudioStreamOn());

      verify(roomService, times(1)).getRoomsIds(currentUser);
      verify(meetingRepository, times(1))
        .getByRoomsIds(List.of(room1Id.toString(), room2Id.toString(), room3Id.toString()));
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If rooms, which user is member of, hasn't any meetings, it returns an empty list")
    public void listMeeting_testUserRoomsHasNoMeetings() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomsIds(currentUser)).thenReturn(List.of(room1Id, room2Id, room3Id));
      when(meetingRepository.getByRoomsIds(List.of(room1Id.toString(), room2Id.toString(), room3Id.toString())))
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
    public void listMeeting_testUserHasNotRooms() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);

      when(roomService.getRoomsIds(currentUser)).thenReturn(List.of());
      when(meetingRepository.getByRoomsIds(List.of())).thenReturn(List.of());

      List<MeetingDto> meetings = meetingService.getMeetings(currentUser);

      assertNotNull(meetings);
      assertEquals(0, meetings.size());
      verify(roomService, times(1)).getRoomsIds(currentUser);
      verify(meetingRepository, times(1))
        .getByRoomsIds(List.of());
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Get meeting by id tests")
  public class GetMeetingByIdTests {

    @Test
    @DisplayName("Returns the required meeting with all participants")
    public void getMeetingById_testOk() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getByUserIdAndRoomId(user1Id, room1Id)).thenReturn(Optional.of(MemberDto.create()));

      MeetingDto meetingDto = meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id));

      assertNotNull(meetingDto);
      assertEquals(meeting1Id, meetingDto.getId());
      assertEquals(room1Id, meetingDto.getRoomId());
      assertNotNull(meetingDto.getParticipants());
      assertEquals(4, meetingDto.getParticipants().size());
      assertEquals(1, (int) meetingDto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).count());
      assertEquals(2, (int) meetingDto.getParticipants().stream()
        .filter(p -> user2Id.equals(p.getUserId())).count());
      assertEquals(1, (int) meetingDto.getParticipants().stream()
        .filter(p -> user3Id.equals(p.getUserId())).count());
      Optional<ParticipantDto> participant1 = meetingDto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertTrue(participant1.get().isVideoStreamOn());
      assertTrue(participant1.get().isAudioStreamOn());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getByUserIdAndRoomId(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room meeting member, it throws a 'forbidden' exception")
    public void getMeetingById_testUserNotRoomMeetingMember() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getByUserIdAndRoomId(user1Id, room1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Forbidden - User '%s' hasn't access to the meeting with id '%s'", user1Id, meeting1Id),
        exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getByUserIdAndRoomId(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the meeting doesn't exists, it throws a 'not found' exception")
    public void getMeetingById_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

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
  public class GetMeetingEntityTests {

    @Test
    @DisplayName("Returns the required meeting entity with all participants")
    public void getMeetingEntity_TestOk() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));

      Optional<Meeting> meeting = meetingService.getMeetingEntity(meeting1Id);
      assertTrue(meeting.isPresent());
      assertEquals(meeting1Id.toString(), meeting.get().getId());
      assertEquals(room1Id.toString(), meeting.get().getRoomId());
      assertNotNull(meeting.get().getParticipants());
      assertEquals(4, meeting.get().getParticipants().size());
      assertEquals(1, (int) meeting.get().getParticipants().stream()
        .filter(p -> user1Id.toString().equals(p.getUserId())).count());
      assertEquals(2, (int) meeting.get().getParticipants().stream()
        .filter(p -> user2Id.toString().equals(p.getUserId())).count());
      assertEquals(1, (int) meeting.get().getParticipants().stream()
        .filter(p -> user3Id.toString().equals(p.getUserId())).count());
      Optional<Participant> participant1 = meeting.get().getParticipants().stream()
        .filter(p -> user1Id.toString().equals(p.getUserId())).findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id.toString(), participant1.get().getUserId());
      assertEquals(session1User1Id, participant1.get().getSessionId());
      assertTrue(participant1.get().hasVideoStreamOn());
      assertTrue(participant1.get().hasAudioStreamOn());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verifyNoMoreInteractions(meetingRepository);
      verifyNoInteractions(membersService, roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the meeting doesn't exists, it throws a 'not found' exception")
    public void getMeetingEntity_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

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
  public class GetMeetingByRoomIdTests {

    @Test
    @DisplayName("Returns the meeting of the required with all participants")
    public void getMeetingByRoomId_testOk() {
      when(roomService.getRoomEntityAndCheckUser(room1Id, UserPrincipal.create(user1Id), false))
        .thenReturn(room1);
      when(meetingRepository.getByRoomId(room1Id.toString())).thenReturn(Optional.of(meeting1));

      MeetingDto meetingDto = meetingService.getMeetingByRoomId(room1Id, UserPrincipal.create(user1Id));

      assertNotNull(meetingDto);
      assertEquals(meeting1Id, meetingDto.getId());
      assertEquals(room1Id, meetingDto.getRoomId());
      assertNotNull(meetingDto.getParticipants());
      assertEquals(4, meetingDto.getParticipants().size());
      assertEquals(1, (int) meetingDto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).count());
      assertEquals(2, (int) meetingDto.getParticipants().stream()
        .filter(p -> user2Id.equals(p.getUserId())).count());
      assertEquals(1, (int) meetingDto.getParticipants().stream()
        .filter(p -> user3Id.equals(p.getUserId())).count());
      Optional<ParticipantDto> participant1 = meetingDto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertTrue(participant1.get().isVideoStreamOn());
      assertTrue(participant1.get().isAudioStreamOn());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(room1Id, UserPrincipal.create(user1Id), false);
      verify(meetingRepository, times(1)).getByRoomId(room1Id.toString());
      verifyNoMoreInteractions(meetingRepository, roomService);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the room meeting doesn't exists, it throws a 'not found' exception")
    public void getMeetingByRoomId_testMeetingNotExists() {
      when(roomService.getRoomEntityAndCheckUser(room1Id, UserPrincipal.create(user1Id), false))
        .thenReturn(room1);
      when(meetingRepository.getByRoomId(room1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        meetingService.getMeetingByRoomId(room1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Meeting of the room with id '%s' doesn't exist", room1Id),
        exception.getMessage());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(room1Id, UserPrincipal.create(user1Id), false);
      verify(meetingRepository, times(1)).getByRoomId(room1Id.toString());
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Gets or creates meeting entity by room id tests")
  public class GetsOrCreatesMeetingEntityByRoomIdTests {

    @Test
    @DisplayName("If meeting exists, correctly returns it")
    public void getsOrCreatesMeetingEntityByRoomId_textOkMeetingExists() {
      UserPrincipal userPrincipal = UserPrincipal.create(user1Id).sessionId(session1User1Id);
      when(roomService.getRoomEntityAndCheckUser(room1Id, userPrincipal, false)).thenReturn(room1);
      when(meetingRepository.getByRoomId(room1Id.toString())).thenReturn(Optional.of(meeting1));

      Meeting meeting = meetingService.getsOrCreatesMeetingEntityByRoomId(room1Id, userPrincipal);
      assertEquals(meeting1, meeting);

      verify(roomService, times(1)).getRoomEntityAndCheckUser(room1Id, userPrincipal, false);
      verify(meetingRepository, times(1)).getByRoomId(room1Id.toString());
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(membersService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If meeting doesn't exist, it correctly creates a new meeting for the specified room and returns it")
    public void getsOrCreatesMeetingEntityByRoomId_testOkMeetingNotExists() {
      UUID meetingId = UUID.randomUUID();
      Meeting meeting = Meeting.create().id(meetingId.toString()).roomId(room1Id.toString());
      UserPrincipal userPrincipal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(room1Id, userPrincipal, false)).thenReturn(room1);
      when(meetingRepository.getByRoomId(room1Id.toString())).thenReturn(Optional.empty());
      when(meetingRepository.insert(room1.getName(),
        MeetingType.PERMANENT,
        room1Id,
        null)
      ).thenReturn(meeting);

      Meeting meetingEntity;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(meetingId);
        uuid.when(() -> UUID.fromString(room1Id.toString())).thenReturn(room1Id);
        uuid.when(() -> UUID.fromString(meetingId.toString())).thenReturn(meetingId);
        meetingEntity = meetingService.getsOrCreatesMeetingEntityByRoomId(room1Id, userPrincipal);
      }

      assertNotNull(meetingEntity);
      assertEquals(meetingId.toString(), meetingEntity.getId());
      assertEquals(room1Id.toString(), meetingEntity.getRoomId());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(room1Id, userPrincipal, false);
      verify(roomService, times(1)).setMeetingIntoRoom(room1, meetingEntity);
      verify(meetingRepository, times(1)).getByRoomId(room1Id.toString());
      verify(meetingRepository, times(1)).insert(room1.getName(),
        MeetingType.PERMANENT,
        room1Id,
        null);
      verify(videoServerService, times(1)).startMeeting(meetingId.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        MeetingCreatedEvent.create(user1Id, null).meetingId(meetingId).roomId(room1Id));
      verifyNoMoreInteractions(roomService, meetingRepository, videoServerService, eventDispatcher);
      verifyNoInteractions(membersService);
    }

  }

  @Nested
  @DisplayName("Delete meeting by id tests")
  public class DeleteMeetingByIdTests {

    @Test
    @DisplayName("Deletes the requested meeting")
    public void deleteMeetingById_testOk() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(roomService.getRoomEntityAndCheckUser(room1Id, UserPrincipal.create(user1Id), false)).thenReturn(room1);

      meetingService.deleteMeetingById(meeting1Id, UserPrincipal.create(user1Id));

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(meetingRepository, times(1)).delete(meeting1);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(room1Id, UserPrincipal.create(user1Id), false);
      verify(videoServerService, times(1)).stopMeeting(meeting1Id.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        MeetingDeletedEvent.create(user1Id, null).meetingId(meeting1Id));
      verifyNoMoreInteractions(meetingRepository, roomService, videoServerService, eventDispatcher);
      verifyNoInteractions(membersService);
    }

    @Test
    @DisplayName("If the meeting doesn't exist, it throws a 'not found' exception")
    public void deleteMeetingById_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        meetingService.deleteMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

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
    @DisplayName("If the authenticated user isn't a room meeting member, it throws a 'forbidden' exception")
    public void getMeetingById_testUserNotRoomMeetingMember() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.of(meeting1));
      when(membersService.getByUserIdAndRoomId(user1Id, room1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Forbidden - User '%s' hasn't access to the meeting with id '%s'", user1Id, meeting1Id),
        exception.getMessage());

      verify(meetingRepository, times(1)).getById(meeting1Id.toString());
      verify(membersService, times(1)).getByUserIdAndRoomId(user1Id, room1Id);
      verifyNoMoreInteractions(meetingRepository, membersService);
      verifyNoInteractions(roomService, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the meeting doesn't exists, it throws a 'not found' exception")
    public void getMeetingById_testMeetingNotExists() {
      when(meetingRepository.getById(meeting1Id.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        meetingService.getMeetingById(meeting1Id, UserPrincipal.create(user1Id)));

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
}
