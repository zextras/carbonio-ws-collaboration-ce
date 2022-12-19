package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.zextras.carbonio.chats.core.data.entity.MeetingBuilder;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantJoinedEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantLeftEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantScreenStreamClosed;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantScreenStreamOpened;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantVideoStreamClosed;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantVideoStreamOpened;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class ParticipantServiceImplTest {

  private final ParticipantService    participantService;
  private final MeetingService        meetingService;
  private final RoomService           roomService;
  private final ParticipantRepository participantRepository;
  private final VideoServerService    videoServerService;
  private final EventDispatcher       eventDispatcher;

  public ParticipantServiceImplTest(MeetingMapper meetingMapper) {
    this.meetingService = mock(MeetingService.class);
    this.roomService = mock(RoomService.class);
    this.participantRepository = mock(ParticipantRepository.class);
    this.videoServerService = mock(VideoServerService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.participantService = new ParticipantServiceImpl(
      this.meetingService,
      this.roomService,
      this.participantRepository,
      meetingMapper,
      this.videoServerService,
      this.eventDispatcher);
  }

  private UUID        user1Id;
  private String      user1Session1;
  private Participant participant1Session1;
  private UUID        user2Id;
  private String      user2Session1;
  private Participant participant2Session1;
  private String      user2Session2;
  private Participant participant2Session2;
  private UUID        user3Id;
  private String      user3Session1;

  private UUID roomId;
  private Room room;

  private UUID    meeting1Id;
  private Meeting meeting1;
  private UUID    meeting2Id;
  private Meeting meeting2;

  @BeforeEach
  public void init() {
    user1Id = UUID.randomUUID();
    user1Session1 = "user1Session1";
    participant1Session1 = ParticipantBuilder.create(Meeting.create(), user1Session1).userId(user1Id)
      .audioStreamOn(true).videoStreamOn(false).screenStreamOn(false)
      .createdAt(OffsetDateTime.parse("2022-01-01T13:32:00Z")).build();
    user2Id = UUID.randomUUID();
    user2Session1 = "user2Session1";
    participant2Session1 = ParticipantBuilder.create(Meeting.create(), user2Session1).userId(user2Id)
      .audioStreamOn(true).videoStreamOn(false).screenStreamOn(false)
      .createdAt(OffsetDateTime.parse("2022-01-01T13:32:00Z")).build();
    user2Session2 = "user2Session2";
    participant2Session2 = ParticipantBuilder.create(Meeting.create(), user2Session2).userId(user2Id)
      .audioStreamOn(true).videoStreamOn(true).screenStreamOn(true)
      .createdAt(OffsetDateTime.parse("2022-01-01T13:32:00Z")).build();
    user3Id = UUID.randomUUID();
    user3Session1 = "user3Session1";

    roomId = UUID.randomUUID();
    room = Room.create();
    room.id(roomId.toString())
      .type(RoomTypeDto.GROUP)
      .name("room1")
      .description("Room one")
      .subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()),
        Subscription.create(room, user3Id.toString())));

    meeting1Id = UUID.randomUUID();
    meeting1 = MeetingBuilder.create(meeting1Id)
      .roomId(roomId)
      .createdAt(OffsetDateTime.parse("2022-01-01T12:00:00Z"))
      .participants(new ArrayList<>(List.of(participant1Session1, participant2Session1, participant2Session2)))
      .build();
    meeting2Id = UUID.randomUUID();
    meeting2 = MeetingBuilder.create(meeting2Id)
      .roomId(roomId)
      .createdAt(OffsetDateTime.parse("2022-01-01T12:00:00Z"))
      .participants(new ArrayList<>(List.of(participant2Session1)))
      .build();
  }

  @Nested
  @DisplayName("Insert meeting participant by room id tests")
  public class InsertMeetingParticipantByRoomIdTests {

    @Test
    @DisplayName("If the meeting exists, it inserts the current user as a meeting participant")
    public void insertMeetingParticipantByRoomId_testOkMeetingExists() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).sessionId(user3Session1);
      when(meetingService.getsOrCreatesMeetingEntityByRoomId(roomId, currentUser)).thenReturn(meeting1);
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      Optional<MeetingDto> meetingDto = participantService.insertMeetingParticipantByRoomId(roomId,
        JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false), currentUser);
      assertTrue(meetingDto.isEmpty());

      verify(meetingService, times(1)).getsOrCreatesMeetingEntityByRoomId(roomId, currentUser);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1))
        .insert(Participant.create(meeting1, user3Session1).userId(user3Id.toString()));
      verify(videoServerService, times(1))
        .joinMeeting(user3Session1, meeting1Id.toString(), false, true);
      verify(eventDispatcher, times(1))
        .sendToUserQueue(List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
          MeetingParticipantJoinedEvent.create(user3Id, user3Session1).meetingId(meeting1Id));
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the meeting doesn't exist, it creates a new meeting and inserts the current user as a meeting participant")
    public void insertMeetingParticipantByRoomId_testOkMeetingNotExists() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).sessionId(user3Session1);
      Meeting meeting = Meeting.create()
        .id(UUID.randomUUID().toString()).roomId(roomId.toString()).participants(new ArrayList<>());
      when(meetingService.getsOrCreatesMeetingEntityByRoomId(roomId, currentUser)).thenReturn(meeting);
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      Optional<MeetingDto> meetingDto = participantService.insertMeetingParticipantByRoomId(roomId,
        JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false), currentUser);
      assertTrue(meetingDto.isPresent());

      verify(meetingService, times(1)).getsOrCreatesMeetingEntityByRoomId(roomId, currentUser);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1))
        .insert(Participant.create(meeting, user3Session1).userId(user3Id.toString()));
      verify(videoServerService, times(1))
        .joinMeeting(user3Session1, meeting.getId(), false, true);
      verify(eventDispatcher, times(1))
        .sendToUserQueue(List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
          MeetingParticipantJoinedEvent.create(user3Id, user3Session1).meetingId(UUID.fromString(meeting.getId())));
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Insert meeting participant tests")
  public class InsertMeetingParticipantTests {

    @Test
    @DisplayName("It inserts the current user as meeting participant")
    public void insertMeetingParticipant_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).sessionId(user3Session1);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      participantService.insertMeetingParticipant(meeting1Id,
        JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false), currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1))
        .insert(Participant.create(meeting1, user3Session1).userId(user3Id.toString()));
      verify(videoServerService, times(1)).joinMeeting(user3Session1, meeting1Id.toString(),
        false, true);
      verify(eventDispatcher, times(1))
        .sendToUserQueue(List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
          MeetingParticipantJoinedEvent.create(user3Id, user3Session1).meetingId(meeting1Id));
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("It inserts the user as meeting participant with another session")
    public void insertMeetingParticipant_testOkSameUserAnotherSession() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).sessionId(user2Session2);
      when(meetingService.getMeetingEntity(meeting2Id)).thenReturn(Optional.of(meeting2));
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      participantService.insertMeetingParticipant(meeting2Id,
        JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false), currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting2Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1))
        .insert(Participant.create(meeting2, user2Session2).userId(user2Id.toString()));
      verify(videoServerService, times(1)).joinMeeting(user2Session2, meeting2Id.toString(),
        false, true);
      verify(eventDispatcher, times(1))
        .sendToUserQueue(List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
          MeetingParticipantJoinedEvent.create(user2Id, user2Session2).meetingId(meeting2Id));
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the current user is already a meeting participant, it throws a 'conflict' exception")
    public void insertMeetingParticipant_testIsAlreadyMeetingParticipant() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).sessionId(user1Session1);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(ConflictException.class, () ->
        participantService.insertMeetingParticipant(meeting1Id,
          JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false), currentUser));

      assertEquals(Status.CONFLICT.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.CONFLICT.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Conflict - Session is already inserted into the meeting", exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Remove meeting participant tests")
  public class RemoveMeetingParticipantTests {

    @Test
    @DisplayName("It removes the current user as meeting participant")
    public void removeMeetingParticipant_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).sessionId(user2Session2);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      participantService.removeMeetingParticipant(meeting1Id, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1)).remove(participant2Session2);
      verify(videoServerService, times(1)).leaveMeeting(user2Session2, meeting1Id.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        MeetingParticipantLeftEvent.create(user2Id, user2Session2).meetingId(meeting1Id));
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("It removes the current user as meeting participant, " +
      "if it's the last one, the meeting is also removed")
    public void removeMeetingParticipant_testOkLastParticipant() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).sessionId(user2Session1);
      when(meetingService.getMeetingEntity(meeting2Id)).thenReturn(Optional.of(meeting2));
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      participantService.removeMeetingParticipant(meeting2Id, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting2Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1)).remove(participant2Session1);
      verify(videoServerService, times(1)).leaveMeeting(user2Session1, meeting2Id.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        MeetingParticipantLeftEvent.create(user2Id, user2Session1).meetingId(meeting2Id));
      verify(meetingService, times(1)).deleteMeeting(meeting2, room, user2Id, user2Session1);
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("It removes all meeting participant’s sessions")
    public void removeMeetingParticipant_testOkAllSessions() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      participantService.removeMeetingParticipant(meeting1Id, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1)).remove(participant2Session1);
      verify(participantRepository, times(1)).remove(participant2Session2);
      verify(videoServerService, times(1))
        .leaveMeeting(user2Session1, meeting1Id.toString());
      verify(videoServerService, times(1))
        .leaveMeeting(user2Session2, meeting1Id.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        MeetingParticipantLeftEvent.create(user2Id, user2Session1).meetingId(meeting1Id));
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        MeetingParticipantLeftEvent.create(user2Id, user2Session2).meetingId(meeting1Id));
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the current user isn't a meeting participant, it throws a 'not found' exception")
    public void removeMeetingParticipant_testIsNotMeetingParticipant() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).sessionId(user3Session1);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.removeMeetingParticipant(meeting1Id, currentUser));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Not Found - Session not found", exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(meetingService, roomService);
      verifyNoInteractions(participantRepository, videoServerService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Enable video stream tests")
  public class EnableVideoStreamTests {

    private final boolean hasVideoStreamOn = true;

    @Test
    @DisplayName("It opens the video stream for the current session")
    public void enableVideoStream_testOkEnableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableVideoStream(meeting1Id, user1Session1, true,
        UserPrincipal.create(user1Id).sessionId(user1Session1));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(participantRepository, times(1)).update(participant1Session1.videoStreamOn(hasVideoStreamOn));
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString()),
        MeetingParticipantVideoStreamOpened
          .create(user1Id, user1Session1).meetingId(meeting1Id).sessionId(user1Session1));
      verify(videoServerService, times(1)).enableVideoStream(user1Session1, meeting1Id.toString(), true);

      verifyNoMoreInteractions(meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("If video stream is already opened for the current session, correctly it ignores")
    public void enableVideoStream_testOkVideoStreamAlreadyOpenWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableVideoStream(meeting1Id, user2Session2, hasVideoStreamOn,
        UserPrincipal.create(user2Id).sessionId(user2Session2));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the current session is not the requested session, it throws a 'bad request' exception")
    public void enableVideoStream_testErrorEnableWithSessionDifferentToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        participantService.enableVideoStream(meeting1Id, user2Session2, hasVideoStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Bad Request - User '%s' cannot enable the video stream of the session '%s'", user1Id,
          user2Session2),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it throws a 'not found' exception")
    public void enableVideoStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableVideoStream(meeting1Id, user3Session1, hasVideoStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Session '%s' not found into meeting '%s'", user3Session1, meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    public void enableVideoStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableVideoStream(meeting1Id, user3Session1, hasVideoStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Meeting '%s' not found", meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Disable video stream tests")
  public class DisableVideoStreamTests {

    private final boolean hasVideoStreamOn = false;

    @Test
    @DisplayName("It closes the video stream for the current session")
    public void disableVideoStream_testOkDisableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableVideoStream(meeting1Id, user2Session2, hasVideoStreamOn,
        UserPrincipal.create(user2Id).sessionId(user2Session2));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(participantRepository, times(1)).update(participant2Session2.videoStreamOn(hasVideoStreamOn));
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString()),
        MeetingParticipantVideoStreamClosed
          .create(user2Id, user2Session2).meetingId(meeting1Id).sessionId(user2Session2));
      verify(videoServerService, times(1)).enableVideoStream(user2Session2, meeting1Id.toString(), false);
      verifyNoMoreInteractions(meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("If video stream is already closed for the current session, correctly it ignores")
    public void disableVideoStream_testOkVideoStreamAlreadyCloseWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableVideoStream(meeting1Id, user1Session1, hasVideoStreamOn,
        UserPrincipal.create(user1Id).sessionId(user1Session1));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("It closes the video stream for another session")
    public void disableVideoStream_testOkDisableWithAnotherSession() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).sessionId(user1Session1);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableVideoStream(meeting1Id, user2Session2, hasVideoStreamOn, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, true);
      verify(participantRepository, times(1)).update(participant2Session2.videoStreamOn(false));
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString()),
        MeetingParticipantVideoStreamClosed
          .create(user1Id, user1Session1).meetingId(meeting1Id).sessionId(user2Session2));
      verify(videoServerService, times(1)).enableVideoStream(user2Session2, meeting1Id.toString(), false);
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If video stream is already closed for another session, correctly it ignores")
    public void disableVideoStream_testOkVideoStreamAlreadyCloseWithAnotherSession() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).sessionId(user1Session1);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableVideoStream(meeting1Id, user2Session1, hasVideoStreamOn, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, true);
      verifyNoMoreInteractions(meetingService, roomService);
      verifyNoInteractions(participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it throws a 'not found' exception")
    public void disableVideoStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableVideoStream(meeting1Id, user3Session1, hasVideoStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Session '%s' not found into meeting '%s'", user3Session1, meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    public void disableVideoStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableVideoStream(meeting1Id, user3Session1, hasVideoStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Meeting '%s' not found", meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Enable screen share stream tests")
  public class EnableScreenShareStreamTests {

    private final boolean hasScreenStreamOn = true;

    @Test
    @DisplayName("It opens the screen share stream for the current session")
    public void enableScreenShareStream_testOkEnableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableScreenShareStream(meeting1Id, user1Session1, true,
        UserPrincipal.create(user1Id).sessionId(user1Session1));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(participantRepository, times(1)).update(participant1Session1.screenStreamOn(hasScreenStreamOn));
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString()),
        MeetingParticipantScreenStreamOpened
          .create(user1Id, user1Session1).meetingId(meeting1Id).sessionId(user1Session1));
      verify(videoServerService, times(1)).enableScreenShareStream(user1Session1, meeting1Id.toString(), true);

      verifyNoMoreInteractions(meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("If screen share stream is already opened for the current session, correctly it ignores")
    public void enableScreenShareStream_testOkScreenShareStreamAlreadyOpenWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableScreenShareStream(meeting1Id, user2Session2, hasScreenStreamOn,
        UserPrincipal.create(user2Id).sessionId(user2Session2));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the current session is not the requested session, it throws a 'bad request' exception")
    public void enableScreenShareStream_testErrorEnableWithSessionDifferentToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        participantService.enableScreenShareStream(meeting1Id, user2Session2, hasScreenStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Bad Request - User '%s' cannot enable the screen share stream of the session '%s'", user1Id,
          user2Session2),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it throws a 'not found' exception")
    public void enableScreenShareStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableScreenShareStream(meeting1Id, user3Session1, hasScreenStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Session '%s' not found into meeting '%s'", user3Session1, meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    public void enableScreenShareStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableScreenShareStream(meeting1Id, user3Session1, hasScreenStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Meeting '%s' not found", meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }

  @Nested
  @DisplayName("Disable screen share stream tests")
  public class DisableScreenShareStreamTests {

    private final boolean hasScreenStreamOn = false;

    @Test
    @DisplayName("It closes the screen share stream for the current session")
    public void disableScreenShareStream_testOkDisableWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableScreenShareStream(meeting1Id, user2Session2, false,
        UserPrincipal.create(user2Id).sessionId(user2Session2));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(participantRepository, times(1)).update(participant2Session2.screenStreamOn(hasScreenStreamOn));
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString()),
        MeetingParticipantScreenStreamClosed
          .create(user2Id, user2Session2).meetingId(meeting1Id).sessionId(user2Session2));
      verify(videoServerService, times(1)).enableScreenShareStream(user2Session2, meeting1Id.toString(), false);
      verifyNoMoreInteractions(meetingService, participantRepository, eventDispatcher, videoServerService);
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("If screen share stream is already closed for the current session, correctly it ignores")
    public void disableScreenShareStream_testOkScreenShareStreamAlreadyCloseWithSessionEqualToCurrent() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableScreenShareStream(meeting1Id, user1Session1, hasScreenStreamOn,
        UserPrincipal.create(user1Id).sessionId(user1Session1));

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("It closes the screen share stream for another session")
    public void disableScreenShareStream_testOkDisableWithAnotherSession() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).sessionId(user1Session1);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableScreenShareStream(meeting1Id, user2Session2, hasScreenStreamOn, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, true);
      verify(participantRepository, times(1)).update(participant2Session2.screenStreamOn(false));
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString()),
        MeetingParticipantScreenStreamClosed
          .create(user1Id, user1Session1).meetingId(meeting1Id).sessionId(user2Session2));
      verify(videoServerService, times(1)).enableScreenShareStream(user2Session2, meeting1Id.toString(), false);
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If screen share stream is already closed for another session, correctly it ignores")
    public void disableScreenShareStream_testOkScreenShareStreamAlreadyCloseWithAnotherSession() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).sessionId(user1Session1);
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      participantService.enableScreenShareStream(meeting1Id, user2Session1, hasScreenStreamOn, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, true);
      verifyNoMoreInteractions(meetingService, roomService);
      verifyNoInteractions(participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it throws a 'not found' exception")
    public void disableScreenShareStream_testErrorSessionNotFoundInMeetingParticipants() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableScreenShareStream(meeting1Id, user3Session1, hasScreenStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Session '%s' not found into meeting '%s'", user3Session1, meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it throws a 'not found' exception")
    public void disableScreenShareStream_testErrorMeetingNotExists() {
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        participantService.enableScreenShareStream(meeting1Id, user3Session1, hasScreenStreamOn,
          UserPrincipal.create(user1Id).sessionId(user1Session1)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
        String.format("Not Found - Meeting '%s' not found", meeting1Id),
        exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, eventDispatcher, videoServerService);
    }
  }
}
