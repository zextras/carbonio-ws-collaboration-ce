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
import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatMessageService {

  // ==================== MESSAGE OPERATIONS ====================

  /**
   * Inserts a system event into the room timeline (e.g. pin/unpin).
   *
   * @param event {@link SystemEvent} to persist
   * @return the persisted {@link SystemEvent}
   */
  SystemEvent insertSystemEvent(SystemEvent event);

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
   * Gets messages for MAM (Message Archive Management) query
   *
   * @param roomId room identifier
   * @param start start time (inclusive, null for no lower bound)
   * @param end end time (exclusive, null for no upper bound)
   * @param limit maximum number of messages
   * @return list of {@link Message}
   */
  List<Message> getMessageHistory(
      String roomId, OffsetDateTime start, OffsetDateTime end, int limit);

  /**
   * Gets messages using a composite cursor (end, endId) to avoid skipping messages that share the
   * same millisecond timestamp across page boundaries (BUG-12).
   *
   * @param roomId room identifier
   * @param start start time (inclusive, null for no lower bound)
   * @param end upper timestamp bound (exclusive)
   * @param endId message ID that anchors the page boundary — combined with end forms the composite
   *     cursor {@code (created_at, id) < (end, endId)}
   * @param limit maximum number of messages
   * @return list of {@link Message}
   */
  List<Message> getMessageHistory(
      String roomId, OffsetDateTime start, OffsetDateTime end, String endId, int limit);

  /**
   * Gets messages with configurable sort order
   *
   * @param roomId room identifier
   * @param start start time (inclusive, null for no lower bound)
   * @param end end time (exclusive, null for no upper bound)
   * @param limit maximum number of messages
   * @param ascending true for oldest-first, false for newest-first
   * @return list of {@link Message}
   */
  List<Message> getMessageHistory(
      String roomId, OffsetDateTime start, OffsetDateTime end, int limit, boolean ascending);

  /**
   * Gets specific messages by their stanza IDs
   *
   * @param roomId room identifier
   * @param stanzaIds list of stanza IDs
   * @return list of {@link Message}
   */
  List<Message> getMessagesByIds(String roomId, List<String> stanzaIds);

  /**
   * Full-text search in messages
   *
   * @param roomId room identifier
   * @param searchText text to search for
   * @param before upper time bound for pagination (null = start from most recent)
   * @param after lower time bound (exclusive); used to hide history the caller has cleared
   * @param limit maximum number of messages
   * @return list of {@link Message}
   */
  List<Message> searchMessages(
      String roomId, String searchText, OffsetDateTime before, OffsetDateTime after, int limit);

  /**
   * Gets messages up to a specific message ID (MongooseIM to-id extension). Used for backfilling
   * when navigating to a specific message (e.g., from search results).
   *
   * @param roomId room identifier
   * @param toId message ID to load up to (inclusive)
   * @param start start time (null for no lower bound)
   * @param limit maximum number of messages
   * @return list of {@link Message}
   */
  List<Message> getMessagesUpToId(String roomId, String toId, OffsetDateTime start, int limit);

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
   * Sets the reaction a user has on a message. A user has a single reaction per message: an
   * existing reaction with a different emoji is replaced.
   *
   * @param roomId room identifier the message must belong to
   * @param messageId message identifier
   * @param userId user identifier
   * @param reaction emoji reaction
   * @return the emoji that was replaced, or empty if this was a fresh reaction or an unchanged
   *     re-send of the same emoji
   */
  Optional<String> addReaction(String roomId, String messageId, String userId, String reaction);

  /**
   * Removes the reaction a user has on a message
   *
   * @param messageId message identifier
   * @param userId user identifier
   * @return the emoji that was removed, or empty if the user had no reaction
   */
  Optional<String> removeReaction(String messageId, String userId);

  /**
   * Gets all reactions for a message
   *
   * @param messageId message identifier
   * @return list of {@link MessageReaction}
   */
  List<MessageReaction> getReactions(String messageId);

  // ==================== PIN OPERATIONS ====================

  /**
   * Pins a message in a room
   *
   * @param roomId room identifier
   * @param messageId message identifier
   * @param userId user identifier of the user pinning the message
   * @return the persisted MESSAGE_PINNED {@link SystemEvent}, whose {@code createdAt} is the pin
   *     time
   */
  SystemEvent pinMessage(String roomId, String messageId, String userId);

  /**
   * Unpins a message from a room
   *
   * @param roomId room identifier
   * @param messageId message identifier
   * @param userId user identifier of the user unpinning the message
   * @return the persisted MESSAGE_UNPINNED {@link SystemEvent}
   */
  SystemEvent unpinMessage(String roomId, String messageId, String userId);

  /**
   * Gets all pinned messages for a room
   *
   * @param roomId room identifier
   * @return list of {@link MessagePin}
   */
  List<MessagePin> getPinnedMessages(String roomId);

  // ==================== READ MARKER OPERATIONS (Smart Markers) ====================

  /**
   * Sets the read marker of a user in a room to the creation time of a timeline item, never moving
   * it backwards
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
   * Gets all read markers for a room (Smart Markers)
   *
   * @param roomId room identifier
   * @return list of {@link ReadMarker}
   */
  List<ReadMarker> getRoomReadMarkers(String roomId);

  // ==================== PRESENCE OPERATIONS ====================

  /**
   * Returns presence summaries for a batch of user identifiers. Users not found are returned as
   * offline with a {@code null} last-seen timestamp.
   *
   * @param userIds collection of user identifiers to query
   * @return list of {@link PresenceSummary}
   */
  List<PresenceSummary> getBatchPresence(Collection<String> userIds);

  // ==================== CONTACT/ROSTER OPERATIONS ====================

  /**
   * Gets roster (contact list) for a user
   *
   * @param userId user identifier
   * @return list of contact user IDs
   */
  List<String> getRoster(String userId);

  /**
   * Adds mutual contact (both directions)
   *
   * @param user1Id first user
   * @param user2Id second user
   */
  void addMutualContact(String user1Id, String user2Id);

  /**
   * Gets users who should receive presence updates for a user
   *
   * @param userId user identifier
   * @return list of user IDs to notify
   */
  List<String> getPresenceSubscribers(String userId);

  // ==================== MEMBERSHIP VALIDATION ====================

  /**
   * Validates that the given user is a member (subscriber) of the given room.
   *
   * @param roomId room identifier
   * @param userId user identifier
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the room does not exist
   * @throws com.zextras.carbonio.chats.core.exception.ForbiddenException if the user is not a
   *     member of the room
   */
  void validateRoomMembership(String roomId, String userId);

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
   * cleared-history instant).
   *
   * @throws com.zextras.carbonio.chats.core.exception.NotFoundException if the target is missing,
   *     in another room or hidden by the user's cleared history
   */
  void validateReplyTarget(String roomId, String userId, String replyToId);
}
