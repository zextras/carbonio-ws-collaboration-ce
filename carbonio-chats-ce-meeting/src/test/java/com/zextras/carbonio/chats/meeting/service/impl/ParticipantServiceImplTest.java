package com.zextras.carbonio.chats.meeting.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.annotations.UnitTest;
import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.data.entity.MeetingBuilder;
import com.zextras.carbonio.chats.meeting.data.entity.Participant;
import com.zextras.carbonio.chats.meeting.data.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.meeting.data.event.MeetingParticipantJoinedEvent;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.chats.meeting.repository.ParticipantRepository;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import com.zextras.carbonio.chats.meeting.service.ParticipantService;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.List;
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

  public ParticipantServiceImplTest() {
    this.meetingService = mock(MeetingService.class);
    this.roomService = mock(RoomService.class);
    this.participantRepository = mock(ParticipantRepository.class);
    this.videoServerService = mock(VideoServerService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.participantService = new ParticipantServiceImpl(
      this.meetingService,
      this.roomService,
      this.participantRepository,
      this.videoServerService,
      this.eventDispatcher);
  }

  private UUID   user1Id;
  private String user1Session1;
  private UUID   user2Id;
  private String user2Session1;
  private String user2Session2;
  private UUID   user3Id;
  private String user3Session1;

  private UUID roomId;
  private Room room;

  private UUID    meetingId;
  private Meeting meeting;


  @BeforeEach
  public void init() {
    user1Id = UUID.randomUUID();
    user1Session1 = "user1Session1";
    user2Id = UUID.randomUUID();
    user2Session1 = "user2Session1";
    user2Session2 = "user2Session2";
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

    meetingId = UUID.randomUUID();
    MeetingBuilder meetingBuilder = MeetingBuilder.create(meetingId);
    meeting = meetingBuilder
      .roomId(roomId)
      .createdAt(OffsetDateTime.parse("2022-01-01T12:00:00Z"))
      .participants(List.of(
        ParticipantBuilder.create(user1Id, meetingBuilder.build(), user1Session1).microphoneOn(true).cameraOn(true)
          .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z")).build(),
        ParticipantBuilder.create(user2Id, meetingBuilder.build(), user2Session1).microphoneOn(false).cameraOn(true)
          .createdAt(OffsetDateTime.parse("2022-01-01T13:30:00Z")).build(),
        ParticipantBuilder.create(user2Id, meetingBuilder.build(), user2Session2).microphoneOn(true).cameraOn(false)
          .createdAt(OffsetDateTime.parse("2022-01-01T13:32:00Z")).build())).build();
  }

  @Nested
  @DisplayName("Insert meeting participant tests")
  public class InsertMeetingParticipantTests {

    @Test
    @DisplayName("It inserts the current user as meeting participant")
    public void insertMeetingParticipant_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).sessionId(user3Session1);
      when(meetingService.getMeetingEntity(meetingId)).thenReturn(meeting);
      when(roomService.getRoomEntityAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      participantService.insertMeetingParticipant(meetingId,
        JoinSettingsDto.create().microphoneOn(true).cameraOn(false), currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meetingId);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, currentUser, false);
      verify(participantRepository, times(1))
        .insertParticipant(Participant.create(user3Id.toString(), meeting, user3Session1));
      verify(videoServerService, times(1)).joinSession(user3Session1);
      verify(eventDispatcher, times(1))
        .sendToUserQueue(List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
          MeetingParticipantJoinedEvent.create(user3Id, user3Session1).meetingId(meetingId));
      verifyNoMoreInteractions(meetingService, roomService, participantRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the current user is already a meeting participant, it throws a 'conflict' exception")
    public void insertMeetingParticipant_testIsAlreadyMeetingParticipant() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).sessionId(user1Session1);
      when(meetingService.getMeetingEntity(meetingId)).thenReturn(meeting);

      ChatsHttpException exception = assertThrows(ConflictException.class, () ->
        participantService.insertMeetingParticipant(meetingId,
          JoinSettingsDto.create().microphoneOn(true).cameraOn(false), currentUser));

      assertEquals(Status.CONFLICT.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.CONFLICT.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Conflict - Session is already inserted into the meeting", exception.getMessage());

      verify(meetingService, times(1)).getMeetingEntity(meetingId);
      verifyNoMoreInteractions(meetingService);
      verifyNoInteractions(roomService, participantRepository, videoServerService, eventDispatcher);
    }

  }


}
