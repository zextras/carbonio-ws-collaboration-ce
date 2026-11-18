// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.model.Membership;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.util.UUID;

@Singleton
public class RoomAccessImpl implements RoomAccess {

  private final RoomRepository roomRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Inject
  public RoomAccessImpl(
      RoomRepository roomRepository, SubscriptionRepository subscriptionRepository) {
    this.roomRepository = roomRepository;
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public Room loadForMember(UUID roomId, UserPrincipal user) {
    return load(roomId, user, false);
  }

  @Override
  public Room loadForOwner(UUID roomId, UserPrincipal user) {
    return load(roomId, user, true);
  }

  @Override
  public void requireMember(UUID roomId, UserPrincipal user) {
    membership(roomId.toString(), user.getId(), String.format("Room '%s'", roomId));
  }

  @Override
  public void requireOwner(UUID roomId, UserPrincipal user) {
    Membership membership =
        membership(roomId.toString(), user.getId(), String.format("Room '%s'", roomId));
    if (!membership.owner()) {
      throw notOwner(user.getId(), roomId.toString());
    }
  }

  @Override
  public VisibilityFloor getVisibilityFloor(String roomId, String userId) {
    return membership(roomId, userId, String.format("Room '%s' not found", roomId)).floor();
  }

  private Room load(UUID roomId, UserPrincipal user, boolean mustBeOwner) {
    Room room =
        roomRepository
            .getById(roomId.toString())
            .orElseThrow(() -> new NotFoundException(String.format("Room '%s'", roomId)));
    Subscription member =
        room.getSubscriptions().stream()
            .filter(subscription -> subscription.getUserId().equals(user.getId()))
            .findAny()
            .orElseThrow(() -> notMember(user.getId(), roomId.toString()));
    if (mustBeOwner && !member.isOwner()) {
      throw notOwner(user.getId(), roomId.toString());
    }
    return room;
  }

  private Membership membership(String roomId, String userId, String roomNotFoundText) {
    return subscriptionRepository
        .getMembership(roomId, userId)
        .orElseThrow(
            () ->
                roomRepository.getById(roomId).isEmpty()
                    ? new NotFoundException(roomNotFoundText)
                    : notMember(userId, roomId));
  }

  private static ForbiddenException notMember(String userId, String roomId) {
    return new ForbiddenException(
        String.format("User '%s' is not a member of room '%s'", userId, roomId));
  }

  private static ForbiddenException notOwner(String userId, String roomId) {
    return new ForbiddenException(
        String.format("User '%s' is not an owner of room '%s'", userId, roomId));
  }
}
