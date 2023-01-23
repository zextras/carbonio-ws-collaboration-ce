// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;

public interface MessageDispatcher extends HealthIndicator {

  /**
   * Creates a room on XMPP server
   *
   * @param room     room entity to save
   * @param senderId creation user
   */
  void createRoom(Room room, String senderId);

  /**
   * Deletes a room on XMPP server
   *
   * @param roomId identifier of the room to delete
   * @param userId operation user
   */
  void deleteRoom(String roomId, String userId);

  /**
   * Sends a message to communicate that the room name has changed
   *
   * @param roomId   room identifier
   * @param senderId operation user
   * @param name     new room name
   */
  void updateRoomName(String roomId, String senderId, String name);

  /**
   * Sends a message to communicate that the room description has changed
   *
   * @param roomId      room identifier
   * @param senderId    operation user
   * @param description new room description
   */
  void updateRoomDescription(String roomId, String senderId, String description);

  /**
   * Sends a message to communicate that the room picture has changed
   *
   * @param roomId      room identifier
   * @param senderId    operation user
   * @param pictureId   new pictures id
   * @param pictureName new pictures name
   */
  void updateRoomPicture(String roomId, String senderId, String pictureId, String pictureName);

  /**
   * Sends a message to communicate that the room picture is deleted
   *
   * @param roomId   room identifier
   * @param senderId operation user
   */
  void deleteRoomPicture(String roomId, String senderId);

  /**
   * Invites a member to join a room on XMPP server
   *
   * @param roomId      room identifier
   * @param senderId    inviting user identifier
   * @param recipientId invited user identifier
   */
  void addRoomMember(String roomId, String senderId, String recipientId);

  /**
   * Removes a member from a room on XMPP server
   *
   * @param roomId     room identifier
   * @param senderId   operation user identifier
   * @param idToRemove identifier of the user to remove
   */
  void removeRoomMember(String roomId, String senderId, String idToRemove);

  /**
   * Sets two users in their respective contacts list so that they can both see each other's presence
   *
   * @param user1id first user identifier
   * @param user2id second user identifier
   */
  void addUsersToContacts(String user1id, String user2id);

  /**
   * Sets the member role
   *
   * @param roomId      room identifier
   * @param senderId    operation user identifier
   * @param recipientId identifier of the user to modify the roles
   * @param isOwner     if true set the user as owner, otherwise as member
   */
  void setMemberRole(String roomId, String senderId, String recipientId, boolean isOwner);
}
