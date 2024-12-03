// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.MeetingBuilder;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.WaitingParticipant;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingWaitingParticipantAccepted;
import com.zextras.carbonio.chats.core.data.event.MeetingWaitingParticipantRejected;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.repository.WaitingParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.WaitingParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.QueueUpdateStatusDto;
import jakarta.ws.rs.core.Response.Status;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WaitingParticipantServiceImplTest {

  private final WaitingParticipantService waitingParticipantService;
  private final MeetingService meetingService;
  private final RoomService roomService;
  private final MembersService membersService;
  private final WaitingParticipantRepository waitingParticipantRepository;
  private final EventDispatcher eventDispatcher;

  public WaitingParticipantServiceImplTest() {
    this.meetingService = mock(MeetingService.class);
    this.roomService = mock(RoomService.class);
    this.membersService = mock(MembersService.class);
    this.waitingParticipantRepository = mock(WaitingParticipantRepository.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.waitingParticipantService =
        new WaitingParticipantServiceImpl(
            meetingService,
            roomService,
            membersService,
            waitingParticipantRepository,
            eventDispatcher);
  }

  private UUID user1Id;
  private UUID user1Queue1;
  private UUID user2Id;
  private UUID user2Queue1;
  private UUID user3Id;
  private UUID user3Queue1;

  private UUID waitingParticipant1Id;

  private UUID scheduledRoomId;
  private Room scheduledRoom;

  private UUID scheduledMeetingId;
  private Meeting scheduledMeeting;

  @BeforeEach
  public void init() {
    user1Id = UUID.randomUUID();
    user1Queue1 = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user2Queue1 = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    user3Queue1 = UUID.randomUUID();

    waitingParticipant1Id = UUID.randomUUID();

    scheduledRoomId = UUID.randomUUID();
    scheduledRoom = Room.create();
    scheduledRoom
        .id(scheduledRoomId.toString())
        .type(RoomTypeDto.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(
            List.of(
                Subscription.create(scheduledRoom, user1Id.toString()).owner(true),
                Subscription.create(scheduledRoom, user2Id.toString())));

    scheduledMeetingId = UUID.randomUUID();
    scheduledMeeting =
        MeetingBuilder.create(scheduledMeetingId)
            .roomId(scheduledRoomId)
            .createdAt(OffsetDateTime.parse("2022-01-01T12:00:00Z"))
            .meetingType(MeetingType.SCHEDULED)
            .participants(
                List.of(
                    Participant.create()
                        .userId(user1Id.toString())
                        .queueId(user1Queue1.toString())))
            .build();
  }

  @AfterEach
  public void cleanUp() {
    verifyNoMoreInteractions(
        meetingService, roomService, membersService, waitingParticipantRepository, eventDispatcher);
    reset(
        meetingService, roomService, membersService, waitingParticipantRepository, eventDispatcher);
  }

  @Nested
  @DisplayName("Add queued user tests")
  class AddQueuedUserTests {

    @Test
    @DisplayName("Add a user to the queue")
    void addQueuedUser_testOk() {
      waitingParticipantService.addQueuedUser(
          scheduledMeetingId.toString(), user1Id.toString(), user1Queue1.toString());

      ArgumentCaptor<WaitingParticipant> wpCaptor =
          ArgumentCaptor.forClass(WaitingParticipant.class);
      verify(waitingParticipantRepository, times(1)).insert(wpCaptor.capture());

      assertEquals(1, wpCaptor.getAllValues().size());
      WaitingParticipant waitingParticipant = wpCaptor.getValue();
      assertFalse(waitingParticipant.getId().isEmpty());
      assertEquals(scheduledMeetingId.toString(), waitingParticipant.getMeetingId());
      assertEquals(user1Id.toString(), waitingParticipant.getUserId());
      assertEquals(user1Queue1.toString(), waitingParticipant.getQueueId());
      assertEquals(JoinStatus.WAITING, waitingParticipant.getStatus());
    }
  }

  @Nested
  @DisplayName("Remove queued user tests")
  class RemoveQueuedUserTests {

    @Test
    @DisplayName("Remove a user from the queue")
    void removeQueuedUser_testOk() {
      WaitingParticipant wp =
          WaitingParticipant.create()
              .id(waitingParticipant1Id.toString())
              .meetingId(scheduledMeetingId.toString())
              .userId(user1Id.toString())
              .queueId(user1Queue1.toString())
              .status(JoinStatus.WAITING);
      waitingParticipantService.removeQueuedUser(wp);

      verify(waitingParticipantRepository, times(1)).remove(wp);
    }
  }

  @Nested
  @DisplayName("Update queued user tests")
  class UpdateQueuedUserTests {

    @Test
    @DisplayName("Update a user in the queue")
    void updateQueuedUser_testOk() {
      WaitingParticipant wp =
          WaitingParticipant.create()
              .id(waitingParticipant1Id.toString())
              .meetingId(scheduledMeetingId.toString())
              .userId(user1Id.toString())
              .queueId(user1Queue1.toString())
              .status(JoinStatus.WAITING);
      waitingParticipantService.updateQueuedUser(wp.status(JoinStatus.ACCEPTED));

      ArgumentCaptor<WaitingParticipant> wpCaptor =
          ArgumentCaptor.forClass(WaitingParticipant.class);
      verify(waitingParticipantRepository, times(1)).update(wpCaptor.capture());

      assertEquals(1, wpCaptor.getAllValues().size());
      WaitingParticipant waitingParticipant = wpCaptor.getValue();
      assertEquals(waitingParticipant1Id.toString(), waitingParticipant.getId());
      assertEquals(scheduledMeetingId.toString(), waitingParticipant.getMeetingId());
      assertEquals(user1Id.toString(), waitingParticipant.getUserId());
      assertEquals(user1Queue1.toString(), waitingParticipant.getQueueId());
      assertEquals(JoinStatus.ACCEPTED, waitingParticipant.getStatus());
    }
  }

  @Nested
  @DisplayName("Get queue tests")
  class GetQueueTests {

    @Test
    @DisplayName("Return empty list if no user in queue")
    void getQueue_testEmpty() {
      when(waitingParticipantRepository.getWaitingList(scheduledMeetingId.toString()))
          .thenReturn(Collections.emptyList());

      List<UUID> queuedUsers = waitingParticipantService.getQueue(scheduledMeetingId);

      assertEquals(Collections.emptyList(), queuedUsers);

      verify(waitingParticipantRepository, times(1)).getWaitingList(scheduledMeetingId.toString());
    }

    @Test
    @DisplayName("Return the list of users in queue")
    void getQueue_testOk() {
      when(waitingParticipantRepository.getWaitingList(scheduledMeetingId.toString()))
          .thenReturn(
              List.of(
                  new WaitingParticipant().userId(user1Id.toString()),
                  new WaitingParticipant().userId(user2Id.toString())));

      List<UUID> queuedUsers = waitingParticipantService.getQueue(scheduledMeetingId);

      assertEquals(2, queuedUsers.size());
      assertEquals(List.of(user1Id, user2Id), queuedUsers);

      verify(waitingParticipantRepository, times(1)).getWaitingList(scheduledMeetingId.toString());
    }
  }

  @Nested
  @DisplayName("Clear queue tests")
  class ClearQueueTests {

    @Test
    @DisplayName("Clear the queue")
    void clearQueue_testOk() {
      waitingParticipantService.clearQueue(scheduledMeetingId);

      verify(waitingParticipantRepository, times(1)).clear(scheduledMeetingId.toString());
    }
  }

  @Nested
  @DisplayName("Remove the user from the queue by its queue id")
  class RemoveQueuedUserWithQueueIdTests {

    @Test
    @DisplayName("Remove the user from the queue by its queue id")
    void removeUserFromQueueWithQueueId_testOk() {
      WaitingParticipant waitingParticipant =
          WaitingParticipant.create()
              .userId(user3Id.toString())
              .queueId(user3Queue1.toString())
              .meetingId(scheduledMeetingId.toString());
      when(waitingParticipantRepository.getByQueueId(user3Queue1.toString()))
          .thenReturn(Optional.of(waitingParticipant));
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));

      waitingParticipantService.removeFromQueue(user3Queue1);

      verify(waitingParticipantRepository, times(1)).getByQueueId(user3Queue1.toString());
      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1)).remove(waitingParticipant);

      DomainEvent event =
          MeetingWaitingParticipantRejected.create()
              .meetingId(scheduledMeetingId)
              .userId(UUID.fromString(user3Id.toString()));
      verify(eventDispatcher).sendToUserExchange(List.of(user1Id.toString()), event);
      verify(eventDispatcher).sendToUserQueue(user3Id.toString(), user3Queue1.toString(), event);
    }
  }

  @Nested
  @DisplayName("Update queue tests")
  class UpdateQueueTests {

    @Test
    @DisplayName("Reject a user from queue")
    void updateQueue_rejectUser() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.getWaitingParticipant(
              scheduledMeetingId.toString(), user3Id.toString()))
          .thenReturn(
              Optional.of(
                  new WaitingParticipant()
                      .userId(user3Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user3Queue1.toString())));

      waitingParticipantService.updateQueue(
          scheduledMeetingId, user3Id, QueueUpdateStatusDto.REJECTED, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1))
          .getWaitingParticipant(scheduledMeetingId.toString(), user3Id.toString());
      verify(waitingParticipantRepository, times(1))
          .remove(
              WaitingParticipant.create()
                  .userId(user3Id.toString())
                  .status(JoinStatus.WAITING)
                  .queueId(user3Queue1.toString()));

      DomainEvent event =
          MeetingWaitingParticipantRejected.create()
              .meetingId(scheduledMeetingId)
              .userId(UUID.fromString(user3Id.toString()));
      verify(eventDispatcher).sendToUserExchange(List.of(user1Id.toString()), event);
      verify(eventDispatcher).sendToUserQueue(user3Id.toString(), user3Queue1.toString(), event);
    }

    @Test
    @DisplayName("Exit from the queue from the user")
    void updateQueue_exitQueueUser() {
      UserPrincipal currentUser = UserPrincipal.create(user3Id).queueId(user3Queue1);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.getWaitingParticipant(
              scheduledMeetingId.toString(), user3Id.toString()))
          .thenReturn(
              Optional.of(
                  new WaitingParticipant()
                      .userId(user3Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user3Queue1.toString())));

      waitingParticipantService.updateQueue(
          scheduledMeetingId, user3Id, QueueUpdateStatusDto.REJECTED, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1))
          .getWaitingParticipant(scheduledMeetingId.toString(), user3Id.toString());
      verify(waitingParticipantRepository, times(1))
          .remove(
              WaitingParticipant.create()
                  .userId(user3Id.toString())
                  .status(JoinStatus.WAITING)
                  .queueId(user3Queue1.toString()));

      DomainEvent event =
          MeetingWaitingParticipantRejected.create()
              .meetingId(scheduledMeetingId)
              .userId(UUID.fromString(user3Id.toString()));
      verify(eventDispatcher).sendToUserExchange(List.of(user1Id.toString()), event);
      verify(eventDispatcher).sendToUserQueue(user3Id.toString(), user3Queue1.toString(), event);
    }

    @Test
    @DisplayName("Accept a user from queue")
    void updateQueue_acceptUser() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id).queueId(user1Queue1);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.getWaitingParticipant(
              scheduledMeetingId.toString(), user3Id.toString()))
          .thenReturn(
              Optional.of(
                  new WaitingParticipant()
                      .userId(user3Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user3Queue1.toString())));

      waitingParticipantService.updateQueue(
          scheduledMeetingId, user3Id, QueueUpdateStatusDto.ACCEPTED, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);
      verify(waitingParticipantRepository, times(1))
          .getWaitingParticipant(scheduledMeetingId.toString(), user3Id.toString());
      verify(waitingParticipantRepository, times(1))
          .update(
              WaitingParticipant.create()
                  .userId(user3Id.toString())
                  .status(JoinStatus.ACCEPTED)
                  .queueId(user3Queue1.toString()));
      verify(membersService, times(1))
          .insertRoomMembers(
              scheduledRoomId,
              List.of(
                  MemberToInsertDto.create().userId(user3Id).owner(false).historyCleared(false)),
              currentUser);

      DomainEvent event =
          MeetingWaitingParticipantAccepted.create()
              .meetingId(scheduledMeetingId)
              .userId(UUID.fromString(user3Id.toString()));
      verify(eventDispatcher).sendToUserExchange(List.of(user1Id.toString()), event);
      verify(eventDispatcher).sendToUserQueue(user3Id.toString(), user3Queue1.toString(), event);
    }

    @Test
    @DisplayName("Block a non owner from moderating the queue")
    void updateQueue_testIsNotOwner() {
      UserPrincipal currentUser = UserPrincipal.create(user2Id).queueId(user2Queue1);
      when(meetingService.getMeetingEntity(scheduledMeetingId))
          .thenReturn(Optional.of(scheduledMeeting));
      when(roomService.getRoom(scheduledRoomId)).thenReturn(Optional.of(scheduledRoom));
      when(waitingParticipantRepository.getWaitingParticipant(
              scheduledMeetingId.toString(), user3Id.toString()))
          .thenReturn(
              Optional.of(
                  new WaitingParticipant()
                      .userId(user3Id.toString())
                      .status(JoinStatus.WAITING)
                      .queueId(user3Queue1.toString())));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  waitingParticipantService.updateQueue(
                      scheduledMeetingId, user3Id, QueueUpdateStatusDto.REJECTED, currentUser));

      verify(meetingService, times(1)).getMeetingEntity(scheduledMeetingId);
      verify(roomService, times(1)).getRoom(scheduledRoomId);

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          "Forbidden - User cannot accept or reject a queued user", exception.getMessage());
    }
  }
}
