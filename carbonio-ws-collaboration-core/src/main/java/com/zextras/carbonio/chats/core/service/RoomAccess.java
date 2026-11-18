// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.util.List;
import java.util.UUID;

/**
 * The only place that decides whether a user is a member or an owner of a room. The loading checks
 * return the room with its subscriptions, for callers that use it; the other checks run one
 * targeted query and load nothing, for callers that discard the room.
 */
public interface RoomAccess {

  /**
   * Loads the room with its subscriptions and checks that the user is a member
   *
   * @throws NotFoundException if the room doesn't exist
   * @throws ForbiddenException if the user isn't a room member
   */
  Room loadForMember(UUID roomId, UserPrincipal user);

  /**
   * Loads the room with its subscriptions and checks that the user is an owner
   *
   * @throws NotFoundException if the room doesn't exist
   * @throws ForbiddenException if the user isn't a room member or isn't a room owner
   */
  Room loadForOwner(UUID roomId, UserPrincipal user);

  /**
   * Checks with one targeted query that the user is a member, without loading the room
   *
   * @throws NotFoundException if the room doesn't exist
   * @throws ForbiddenException if the user isn't a room member
   */
  void requireMember(UUID roomId, UserPrincipal user);

  /**
   * Checks with one targeted query that the user is an owner, without loading the room
   *
   * @throws NotFoundException if the room doesn't exist
   * @throws ForbiddenException if the user isn't a room member or isn't a room owner
   */
  void requireOwner(UUID roomId, UserPrincipal user);

  /**
   * Checks with one targeted query that the user is a member and returns the cleared-history
   * instant of the user in the room, for the chat paths that also filter by it
   *
   * @throws NotFoundException if the room doesn't exist
   * @throws ForbiddenException if the user isn't a room member
   */
  VisibilityFloor getVisibilityFloor(String roomId, String userId);

  /**
   * Returns the identifiers of every member of the room when the user is one of them, otherwise an
   * empty list; it never throws, also for a room that doesn't exist
   */
  List<String> getMemberIdsIfMember(String roomId, String userId);
}
