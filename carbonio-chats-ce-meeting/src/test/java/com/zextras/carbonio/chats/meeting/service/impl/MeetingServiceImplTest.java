package com.zextras.carbonio.chats.meeting.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.annotations.UnitTest;
import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.data.event.MeetingCreatedEvent;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import com.zextras.carbonio.chats.model.RoomTypeDto;
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
  private final VideoServerService videoServerService;
  private final EventDispatcher    eventDispatcher;

  public MeetingServiceImplTest() {
    this.meetingRepository = mock(MeetingRepository.class);
    this.roomService = mock(RoomService.class);
    this.videoServerService = mock(VideoServerService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.meetingService = new MeetingServiceImpl(
      this.meetingRepository,
      this.roomService,
      this.videoServerService,
      this.eventDispatcher);
  }

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;

  private UUID room1Id;
  private Room room1;

  @BeforeEach
  public void init() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();

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
  }

  @Nested
  @DisplayName("Create meeting by room tests")
  class CreateMeetingByRoomTests {

    @Test
    @DisplayName("It creates a meeting for specified room and returns it")
    public void createMeetingByRoomTests_testOk() {
      UUID meetingId = UUID.randomUUID();
      Meeting meeting = Meeting.create().id(meetingId.toString()).roomId(room1Id.toString());
      UserPrincipal userPrincipal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(room1Id, userPrincipal, false)).thenReturn(room1);
      when(meetingRepository.getMeetingByRoomId(room1Id.toString())).thenReturn(Optional.empty());
      when(meetingRepository.insert(meeting)).thenReturn(meeting);

      MeetingDto meetingDto;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(meetingId);
        uuid.when(() -> UUID.fromString(meeting.getId())).thenReturn(meetingId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
        uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
        meetingDto = meetingService.createMeetingByRoom(room1Id, userPrincipal);
      }

      assertNotNull(meetingDto);
      assertEquals(meetingId, meetingDto.getId());
      assertEquals(room1Id, meetingDto.getRoomId());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(room1Id, userPrincipal, false);
      verify(meetingRepository, times(1)).getMeetingByRoomId(room1Id.toString());
      verify(meetingRepository, times(1)).insert(meeting);
      verify(videoServerService, times(1)).createMeeting(meetingId.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        MeetingCreatedEvent.create(user1Id, null).meetingId(meetingId).roomId(room1Id));
      verifyNoMoreInteractions(roomService, meetingRepository, videoServerService, eventDispatcher);
    }

    @Test
    @DisplayName("If the room has a meeting, it throws a 'conflict' exception")
    public void createMeetingByRoomTests_testRoomMeetingExists() {
      UserPrincipal userPrincipal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(room1Id, userPrincipal, false)).thenReturn(room1);
      when(meetingRepository.getMeetingByRoomId(room1Id.toString())).thenReturn(Optional.of(Meeting.create()));

      ChatsHttpException exception = assertThrows(ConflictException.class, () ->
        meetingService.createMeetingByRoom(room1Id, userPrincipal));

      assertEquals(Status.CONFLICT.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.CONFLICT.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Conflict - Meeting for room '%s' exists", room1Id), exception.getMessage());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(room1Id, userPrincipal, false);
      verify(meetingRepository, times(1)).getMeetingByRoomId(room1Id.toString());
      verifyNoMoreInteractions(roomService, meetingRepository);
      verifyNoInteractions(videoServerService, eventDispatcher);
    }

  }
}
