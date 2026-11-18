// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.util.UUID;

/**
 * Owns the cascades that span rooms, meetings, participants, attachments and storage. Each cascade
 * runs its database steps in one transaction and sends events and calls the storage only after the
 * commit; no transaction wraps the whole cascade.
 */
public interface RoomLifecycleService {

  /**
   * Deletes the specified room with its meeting, participants, attachments and picture
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}, who must be a room owner
   */
  void deleteRoom(UUID roomId, UserPrincipal currentUser);

  /**
   * Deletes the room, with its meeting, participants, attachments and picture, without checking
   * that the user is a member or an owner
   *
   * @param room room to delete {@link Room }
   * @param currentUser user on whose behalf the room is deleted {@link UserPrincipal}
   */
  void deleteRoom(Room room, UserPrincipal currentUser);

  /**
   * Removes a member from the specified room. If the specified user is different from the
   * requester, this action is considered as a kick. Removing the last member deletes the room
   *
   * @param roomId room identifier {@link UUID }
   * @param userId user identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void removeMember(UUID roomId, UUID userId, UserPrincipal currentUser);
}
