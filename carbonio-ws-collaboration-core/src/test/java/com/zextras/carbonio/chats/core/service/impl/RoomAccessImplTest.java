// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.model.Membership;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.Response.Status;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class RoomAccessImplTest {

  private final RoomRepository roomRepository = mock(RoomRepository.class);
  private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
  private final RoomAccess roomAccess = new RoomAccessImpl(roomRepository, subscriptionRepository);

  private final UUID ownerId = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
  private final UUID memberId = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");
  private final UUID outsiderId = UUID.fromString("ea7b9b61-bef5-4cf4-80cb-19612c42593a");
  private final UUID roomId = UUID.fromString("cdc44826-23b0-4e99-bec2-7fb2f00b6b13");
  private final Room room = roomWithOwnerAndMember();

  private Room roomWithOwnerAndMember() {
    Room created = Room.create();
    created
        .id(roomId.toString())
        .type(RoomType.GROUP)
        .name("room1")
        .description("Room one")
        .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
        .subscriptions(
            List.of(
                Subscription.create(created, ownerId.toString()).owner(true),
                Subscription.create(created, memberId.toString()).owner(false)));
    return created;
  }

  private void assertForbidden(ChatsHttpException exception, String message) {
    assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
    assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
    assertEquals(message, exception.getMessage());
  }

  private void assertNotFound(ChatsHttpException exception, String message) {
    assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
    assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
    assertEquals(message, exception.getMessage());
  }

  private String notMemberText(UUID userId) {
    return String.format("Forbidden - User '%s' is not a member of room '%s'", userId, roomId);
  }

  private String notOwner(UUID userId) {
    return String.format("Forbidden - User '%s' is not an owner of room '%s'", userId, roomId);
  }

  @Nested
  @DisplayName("loadForMember and loadForOwner")
  class Loading {

    @Test
    @DisplayName("Returns the required room with all members")
    void loadForMember_ok() {
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      Room loaded = roomAccess.loadForMember(roomId, UserPrincipal.create(memberId));

      assertSame(room, loaded);
      assertEquals(2, loaded.getSubscriptions().size());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), loaded.getPictureUpdatedAt());
      verify(roomRepository, times(1)).getById(roomId.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("Returns the required room with no profile picture with all members")
    void loadForMember_okWithoutPicture() {
      room.pictureUpdatedAt(null);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      Room loaded = roomAccess.loadForMember(roomId, UserPrincipal.create(ownerId));

      assertEquals(roomId.toString(), loaded.getId());
      assertEquals(2, loaded.getSubscriptions().size());
      assertNull(loaded.getPictureUpdatedAt());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void loadForMember_roomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomAccess.loadForMember(roomId, UserPrincipal.create(memberId)));

      assertNotFound(exception, String.format("Not Found - Room '%s'", roomId));
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void loadForMember_notAMember() {
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomAccess.loadForMember(roomId, UserPrincipal.create(outsiderId)));

      assertForbidden(exception, notMemberText(outsiderId));
    }

    @Test
    @DisplayName("The owner variant returns the room to an owner")
    void loadForOwner_ok() {
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      assertSame(room, roomAccess.loadForOwner(roomId, UserPrincipal.create(ownerId)));
    }

    @Test
    @DisplayName("If the user isn't a room owner, it throws a 'forbidden' exception")
    void loadForOwner_notAnOwner() {
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomAccess.loadForOwner(roomId, UserPrincipal.create(memberId)));

      assertForbidden(exception, notOwner(memberId));
    }

    @Test
    @DisplayName("The owner variant tells a non-member from a non-owner")
    void loadForOwner_notAMember() {
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomAccess.loadForOwner(roomId, UserPrincipal.create(outsiderId)));

      assertForbidden(exception, notMemberText(outsiderId));
    }

    @Test
    @DisplayName("The owner variant on a missing room is a 'not found' exception")
    void loadForOwner_roomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomAccess.loadForOwner(roomId, UserPrincipal.create(ownerId)));

      assertNotFound(exception, String.format("Not Found - Room '%s'", roomId));
    }
  }

  @Nested
  @DisplayName("requireMember and requireOwner")
  class Targeted {

    private void membership(UUID userId, Optional<Membership> membership) {
      when(subscriptionRepository.getMembership(roomId.toString(), userId.toString()))
          .thenReturn(membership);
    }

    @Test
    @DisplayName("A member passes with one targeted query and no room loaded")
    void requireMember_ok() {
      membership(memberId, Optional.of(new Membership(false, null)));

      roomAccess.requireMember(roomId, UserPrincipal.create(memberId));

      verify(subscriptionRepository, times(1))
          .getMembership(roomId.toString(), memberId.toString());
      verifyNoMoreInteractions(subscriptionRepository);
      verifyNoInteractions(roomRepository);
    }

    @Test
    @DisplayName("A non-member of an existing room is forbidden, with the loading check text")
    void requireMember_notAMember() {
      membership(outsiderId, Optional.empty());
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomAccess.requireMember(roomId, UserPrincipal.create(outsiderId)));

      assertForbidden(exception, notMemberText(outsiderId));
    }

    @Test
    @DisplayName("A missing room is a 'not found' exception, with the loading check text")
    void requireMember_roomNotExists() {
      membership(memberId, Optional.empty());
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomAccess.requireMember(roomId, UserPrincipal.create(memberId)));

      assertNotFound(exception, String.format("Not Found - Room '%s'", roomId));
    }

    @Test
    @DisplayName("An owner passes requireOwner with one targeted query and no room loaded")
    void requireOwner_ok() {
      membership(ownerId, Optional.of(new Membership(true, null)));

      roomAccess.requireOwner(roomId, UserPrincipal.create(ownerId));

      verify(subscriptionRepository, times(1)).getMembership(roomId.toString(), ownerId.toString());
      verifyNoMoreInteractions(subscriptionRepository);
      verifyNoInteractions(roomRepository);
    }

    @Test
    @DisplayName("A member who is not an owner is forbidden by requireOwner")
    void requireOwner_notAnOwner() {
      membership(memberId, Optional.of(new Membership(false, null)));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomAccess.requireOwner(roomId, UserPrincipal.create(memberId)));

      assertForbidden(exception, notOwner(memberId));
      verifyNoInteractions(roomRepository);
    }

    @Test
    @DisplayName("A non-member is forbidden by requireOwner as a non-member")
    void requireOwner_notAMember() {
      membership(outsiderId, Optional.empty());
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomAccess.requireOwner(roomId, UserPrincipal.create(outsiderId)));

      assertForbidden(exception, notMemberText(outsiderId));
    }

    @Test
    @DisplayName("A missing room is a 'not found' exception for requireOwner")
    void requireOwner_roomNotExists() {
      membership(ownerId, Optional.empty());
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomAccess.requireOwner(roomId, UserPrincipal.create(ownerId)));

      assertNotFound(exception, String.format("Not Found - Room '%s'", roomId));
    }
  }

  @Nested
  @DisplayName("getVisibilityFloor")
  class Floor {

    @Test
    @DisplayName("Returns the cleared-history instant of a member from the same single query")
    void member() {
      OffsetDateTime clearedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      when(subscriptionRepository.getMembership(roomId.toString(), memberId.toString()))
          .thenReturn(Optional.of(new Membership(false, clearedAt)));

      VisibilityFloor floor = roomAccess.getVisibilityFloor(roomId.toString(), memberId.toString());

      assertEquals(clearedAt, floor.clearedAt());
      verify(subscriptionRepository, times(1))
          .getMembership(roomId.toString(), memberId.toString());
      verifyNoInteractions(roomRepository);
    }

    @Test
    @DisplayName("Throws NotFound with the chat text when the room does not exist")
    void roomNotFound() {
      when(subscriptionRepository.getMembership(roomId.toString(), memberId.toString()))
          .thenReturn(Optional.empty());
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomAccess.getVisibilityFloor(roomId.toString(), memberId.toString()));

      assertNotFound(exception, String.format("Not Found - Room '%s' not found", roomId));
    }

    @Test
    @DisplayName("Throws Forbidden when the user is not a member of an existing room")
    void notMember() {
      when(subscriptionRepository.getMembership(roomId.toString(), outsiderId.toString()))
          .thenReturn(Optional.empty());
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomAccess.getVisibilityFloor(roomId.toString(), outsiderId.toString()));

      assertForbidden(exception, notMemberText(outsiderId));
    }

    @Test
    @DisplayName("Never loads the room of a member")
    void neverLoadsTheRoom() {
      when(subscriptionRepository.getMembership(roomId.toString(), memberId.toString()))
          .thenReturn(Optional.of(new Membership(true, null)));

      assertTrue(
          roomAccess
              .getVisibilityFloor(roomId.toString(), memberId.toString())
              .canAccess(OffsetDateTime.now()));
      verify(roomRepository, never()).getById(roomId.toString());
    }
  }
}
