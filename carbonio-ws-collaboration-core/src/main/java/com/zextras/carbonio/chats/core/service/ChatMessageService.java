// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.InboxLastItem;
import com.zextras.carbonio.chats.core.data.model.MessageSearchPage;
import com.zextras.carbonio.chats.core.data.model.PinResult;
import com.zextras.carbonio.chats.core.data.model.PinnedMessagesView;
import com.zextras.carbonio.chats.core.data.model.ReadMarkersView;
import com.zextras.carbonio.chats.core.data.model.TimelineView;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.Removed;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.SetResult;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The message service: every message operation of the REST API checks the membership of the actor,
 * writes, builds and dispatches the WebSocket events after the write and calls the {@link
 * com.zextras.carbonio.chats.core.spi.MessageSentListener}s where the SPI documents it.
 */
public interface ChatMessageService {

  // ==================== MESSAGE OPERATIONS ====================

  /**
   * Sends a text message. With a non-blank {@code tempId} the send is idempotent: a retry of the
   * same {@code (room, sender, tempId)} within the dedup window returns the message of the first
   * request and sends, broadcasts and notifies nothing.
   *
   * @param text the message text; blank is rejected
   * @param replyToId the message being replied to, or null
   * @param tempId the client-generated identifier of the send, or null
   * @return the sent message, or the message of the first request on a duplicate
   * @throws com.zextras.carbonio.chats.core.exception.BadRequestException if the text is blank
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException "Not a room member or
   *     message ID conflict" if the sender is not a member of the room
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException "Reply target not found" if
   *     the reply target is missing, in another room or hidden by the sender's cleared history
   * @throws com.zextras.carbonio.chats.core.exception.ConflictException if the first request with
   *     this {@code tempId} has not completed yet
   */
  Message sendMessage(
      String roomId,
      UserPrincipal sender,
      @Nullable String text,
      @Nullable String replyToId,
      @Nullable String tempId);

  /**
   * Stores an attachment and sends the message that carries it. The membership is checked once,
   * before the {@code tempId} claim and before the file is stored; a non-member claims nothing. The
   * claim is released when no message is created. A duplicate {@code tempId} returns the message of
   * the first request and stores, broadcasts and notifies nothing.
   *
   * @return the created message, or the message of the first request on a duplicate
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException "Room 'x'" (no tempId, no
   *     reply) or "Room 'x' not found" if the room does not exist; "Message 'x' not found" if the
   *     reply target is missing, in another room or hidden by the sender's cleared history
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the sender is not a
   *     member of the room
   * @throws com.zextras.carbonio.chats.core.exception.ConflictException if the first request with
   *     this {@code tempId} has not completed yet
   */
  Message uploadAttachment(
      String roomId,
      InputStream file,
      String mimeType,
      long contentLength,
      String fileName,
      @Nullable String description,
      @Nullable String replyToId,
      @Nullable String area,
      @Nullable String tempId,
      UserPrincipal sender);

  /**
   * Edits the text of a message of the actor and broadcasts the edit: to every room member when the
   * message is the room pin, otherwise to the members who can see the message.
   *
   * @return the edited message
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException "Not a room member, not
   *     the sender, or message is deleted"
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the message is at or
   *     before the actor's cleared-history instant
   */
  Message editMessage(String roomId, String messageId, UserPrincipal editor, String newText);

  /**
   * Soft-deletes a message of the actor and broadcasts the deletion to the members who can see it;
   * when the message was the room pin, the unpin is broadcast to every room member too. The
   * attachment file, if any, stays in place.
   *
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException "Not a room member, not
   *     the sender, or message already deleted"
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the message is at or
   *     before the actor's cleared-history instant
   */
  void deleteMessage(String roomId, String messageId, UserPrincipal deleter);

  /**
   * Forwards a message to another room: the attachment blob, if any, is copied before the write
   * (and discarded with its metadata when the write is rejected or fails), the message is written
   * in one statement and broadcast to the members of the target room.
   *
   * @return the forwarded message in the target room
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if a room does not exist or
   *     the message is not visible in the source room
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of a room
   */
  Message forwardMessage(
      String sourceRoomId, String messageId, String targetRoomId, UserPrincipal user);

  /**
   * Pages the timeline of a room for a member: the items, the data to render them and the read
   * markers, with one membership check.
   */
  TimelineView getTimeline(
      String roomId,
      String userId,
      @Nullable OffsetDateTime before,
      @Nullable String beforeId,
      @Nullable OffsetDateTime after,
      @Nullable String afterId,
      @Nullable OffsetDateTime around,
      @Nullable String aroundId,
      int limit);

  /**
   * Searches the messages of a room visible to a member, one extra hit past {@code limit} telling
   * whether there are more, with one membership check.
   */
  MessageSearchPage searchVisibleMessages(
      String roomId,
      String userId,
      String searchText,
      @Nullable OffsetDateTime before,
      @Nullable String beforeId,
      int limit);

  /**
   * Inserts a system event into the room timeline (e.g. pin/unpin).
   *
   * @param event {@link SystemEvent} to persist
   * @return the persisted {@link SystemEvent}
   */
  SystemEvent insertSystemEvent(SystemEvent event);

  /**
   * Updates an already persisted system event.
   *
   * @param event {@link SystemEvent} to update
   * @return the updated {@link SystemEvent}
   */
  SystemEvent updateSystemEvent(SystemEvent event);

  /**
   * Retrieves all system events for a room ordered by {@code createdAt} ascending.
   *
   * @param roomId room identifier
   * @return {@link List} of {@link SystemEvent}
   */
  List<SystemEvent> getSystemEvents(String roomId);

  /**
   * Gets a message by ID
   *
   * @param messageId message identifier
   * @return the {@link Message} if found
   */
  Optional<Message> getMessage(String messageId);

  /**
   * Gets specific messages by their identifiers
   *
   * @param roomId room identifier
   * @param messageIds list of message identifiers
   * @return list of {@link Message}
   */
  List<Message> getMessagesByIds(String roomId, List<String> messageIds);

  /**
   * Full-text search in messages
   *
   * @param roomId room identifier
   * @param searchText text to search for
   * @param before upper time bound for pagination (null = start from most recent)
   * @param beforeId message that anchors the composite cursor with {@code before}; its stored
   *     creation time replaces {@code before} when it exists in the room (null = time only)
   * @param after lower time bound (exclusive); used to hide history the caller has cleared
   * @param limit maximum number of messages
   * @return list of {@link Message}
   */
  List<Message> searchMessages(
      String roomId,
      String searchText,
      OffsetDateTime before,
      String beforeId,
      OffsetDateTime after,
      int limit);

  // ==================== INBOX OPERATIONS ====================

  int UNREAD_CAP = 1000;

  /**
   * Gets, for each room the user is subscribed to, the newest visible timeline item (message or
   * system event). Rooms without visible items are absent. Ties on the creation time are resolved
   * by id, like the timeline ordering.
   *
   * @param userId user identifier
   * @return map of roomId to {@link InboxLastItem}
   */
  Map<String, InboxLastItem> getInboxLastItems(String userId);

  /**
   * Returns the unread items count (messages and system events not caused by the user) for each
   * room the user is subscribed to, capped at {@link #UNREAD_CAP}.
   *
   * @param userId user identifier
   * @return map of roomId to unread count (rooms with zero unread are absent)
   */
  Map<String, Long> getUnreadCounts(String userId);

  /**
   * Returns all read markers for a user across all rooms, keyed by roomId.
   *
   * @param userId user identifier
   * @return map of roomId to {@link ReadMarker}
   */
  Map<String, ReadMarker> getReadMarkersByUserId(String userId);

  // ==================== REACTION OPERATIONS ====================

  /**
   * Sets the reaction a user has on a message and broadcasts the change to the members who can see
   * the message (nothing when it is unchanged). A user has a single reaction per message: it is
   * created, replaced when the emoji differs, or left untouched when it is the same.
   *
   * @param roomId room identifier the message must belong to
   * @param messageId message identifier
   * @param userId user identifier
   * @param reaction emoji reaction
   * @return the outcome with the previous emoji and the stored updatedAt
   */
  SetResult setReaction(String roomId, String messageId, String userId, String reaction);

  /**
   * Sets the reaction like {@link #setReaction(String, String, String, String)} and then calls the
   * message-sent listeners with the emoji, also when the same emoji is set again.
   */
  SetResult setReaction(String roomId, String messageId, UserPrincipal user, String reaction);

  /**
   * Removes the reaction a user has on a message and broadcasts the removal to the members who can
   * see the message. The message must be in the room and visible to the user (not at or before
   * their cleared-history instant); a user has a single reaction per message, so no emoji is
   * needed. The message-sent listeners are never called.
   *
   * @param roomId room identifier the message must belong to
   * @param messageId message identifier
   * @param userId user identifier
   * @return the removed emoji and the database time of the delete, or empty if the user had no
   *     reaction
   */
  Optional<Removed> removeReaction(String roomId, String messageId, String userId);

  /**
   * Gets all reactions for a message
   *
   * @param messageId message identifier
   * @return list of {@link MessageReaction}
   */
  List<MessageReaction> getReactions(String messageId);

  // ==================== PIN OPERATIONS ====================

  /**
   * Pins a message in a room and broadcasts the pin to every room member
   *
   * @param roomId room identifier
   * @param messageId message identifier
   * @param userId user identifier of the user pinning the message
   * @return the pinned {@link Message} and the persisted MESSAGE_PINNED {@link SystemEvent}, whose
   *     {@code createdAt} is the pin time; a missing, deleted, foreign or hidden message is not
   *     found
   */
  PinResult pinMessage(String roomId, String messageId, String userId);

  /**
   * Unpins a message from a room and broadcasts the unpin to every room member
   *
   * @param roomId room identifier
   * @param messageId message identifier
   * @param userId user identifier of the user unpinning the message, who must be a room member
   * @return the persisted MESSAGE_UNPINNED {@link SystemEvent}, or empty when the message is not
   *     the room pin (nothing is written)
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the room does not exist
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of the room
   */
  Optional<SystemEvent> unpinMessage(String roomId, String messageId, String userId);

  /**
   * Gets all pinned messages for a room
   *
   * @param roomId room identifier
   * @param userId user identifier of the requester, who must be a room member
   * @return list of {@link MessagePin}
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the room does not exist
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of the room
   */
  List<MessagePin> getPinnedMessages(String roomId, String userId);

  /**
   * Gets the pinned messages of a room with the data to render them for the requester, with one
   * membership check.
   *
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the room does not exist
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of the room
   */
  PinnedMessagesView getPinnedMessagesView(String roomId, String userId);

  // ==================== READ MARKER OPERATIONS (Smart Markers) ====================

  /**
   * Sets the read marker of a user in a room to the creation time of a timeline item, never moving
   * it backwards, and broadcasts the update to the members who can see the item when it moved
   *
   * @param roomId room identifier
   * @param userId user identifier
   * @param itemId identifier of the last read message or system event
   * @return the new marker time, or empty when the marker did not move
   */
  Optional<OffsetDateTime> setReadMarker(String roomId, String userId, String itemId);

  /**
   * Gets read marker for a user in a room
   *
   * @param roomId room identifier
   * @param userId user identifier
   * @return the {@link ReadMarker} if exists
   */
  Optional<ReadMarker> getReadMarker(String roomId, String userId);

  /**
   * Gets all read markers for a room (Smart Markers) with the floor of the requester
   *
   * @param roomId room identifier
   * @param userId user identifier of the requester, who must be a room member
   * @return the {@link ReadMarker}s and the visibility floor to clamp them to
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the room does not exist
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of the room
   */
  ReadMarkersView getRoomReadMarkers(String roomId, String userId);

  // ==================== MEMBERSHIP VALIDATION ====================

  /**
   * Gets the visibility floor of a room member and, in the same query, checks the membership.
   *
   * @param roomId room identifier
   * @param userId user identifier
   * @return the {@link VisibilityFloor} (cleared-history instant) of the user in the room
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the room does not exist
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of the room
   */
  VisibilityFloor getVisibilityFloor(String roomId, String userId);

  /**
   * Validates that a reply target is in the room and visible to the user (created after the user's
   * cleared-history instant), checking the membership first.
   *
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the room does not exist
   *     or the target is missing, in another room or hidden by the user's cleared history
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of the room
   */
  void validateReplyTarget(String roomId, String userId, String replyToId);

  /**
   * Validates that a reply target is in the room and visible to a member with the given floor
   * (created after the cleared-history instant).
   *
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the target is missing,
   *     in another room or hidden by the cleared history
   */
  void validateReplyTarget(String roomId, VisibilityFloor floor, String replyToId);
}
