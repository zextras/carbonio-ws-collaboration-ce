// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface MessageDispatcher extends HealthIndicator {

  /**
   * Creates a room on XMPP server
   *
   * @param roomId identifier of the room to create
   * @param senderId creation user
   * @param memberIds identifiers of the members invited in this room
   */
  void createRoom(String roomId, String senderId, List<String> memberIds);

  /**
   * Sends a message to communicate that the room name has changed
   *
   * @param roomId room identifier
   * @param senderId operation user
   * @param name new room name
   */
  void updateRoomName(String roomId, String senderId, String name);

  /**
   * Sends a message to communicate that the room description has changed
   *
   * @param roomId room identifier
   * @param senderId operation user
   * @param description new room description
   */
  void updateRoomDescription(String roomId, String senderId, String description);

  /**
   * Sends a message to communicate that the room picture has changed
   *
   * @param roomId room identifier
   * @param senderId operation user
   * @param pictureId new pictures id
   * @param pictureName new pictures name
   */
  void updateRoomPicture(String roomId, String senderId, String pictureId, String pictureName);

  /**
   * Sends a message to communicate that the room picture is deleted
   *
   * @param roomId room identifier
   * @param senderId operation user
   */
  void deleteRoomPicture(String roomId, String senderId);

  /**
   * Invites a member to join a room on XMPP server
   *
   * @param roomId room identifier
   * @param senderId inviting user identifier
   * @param memberId identifier of the user to add
   */
  void addRoomMember(String roomId, String senderId, String memberId);

  /**
   * Removes a member from a room on XMPP server. It will destroy the room automatically when the
   * last member is removed. Moreover, also the inbox entries and all room messages will be deleted.
   *
   * @param roomId room identifier
   * @param memberId identifier of the user to remove
   */
  void removeRoomMember(String roomId, String memberId);

  /**
   * Sends an affiliation message to a room on XMPP server
   *
   * @param roomId room identifier
   * @param senderId operation user identifier
   * @param memberId identifier of the related room member
   * @param messageType type of the message
   */
  void sendAffiliationMessage(
      String roomId, String senderId, String memberId, MessageType messageType);

  /**
   * Sets two users in their respective contacts list so that they can both see each other's
   * presence
   *
   * @param user1id first user identifier
   * @param user2id second user identifier
   */
  void addUsersToContacts(String user1id, String user2id);

  /**
   * Sends the attachment
   *
   * @param roomId room identifier
   * @param senderId operation user identifier
   * @param fileId identifier of the file
   * @param fileName name of the file
   * @param mimeType mime type of the file
   * @param originalSize size of the file
   * @param description description of the attachment
   * @param messageId identifier of XMPP message to create
   * @param replyId identifier of the message being replied to
   */
  void sendAttachment(
      String roomId,
      String senderId,
      String fileId,
      String fileName,
      String mimeType,
      long originalSize,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area);

  /**
   * Forwards a message
   *
   * @param roomId room identifier
   * @param senderId operation user identifier
   * @param messageToForward message to forward
   * @param fileMetadata file properties {@link FileMetadata}
   */
  void forwardMessage(
      String roomId,
      String senderId,
      ForwardMessageDto messageToForward,
      @Nullable FileMetadata fileMetadata);

  /**
   * Returns the attachment identifier into the message if it exists
   *
   * @param message message to parse
   * @return The attachment identifier of the message wrapped into an {@link Optional} only if it
   *     exists
   */
  Optional<String> getAttachmentIdFromMessage(String message);
}
