// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.model.InboxLastItemRefs;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MessageRepository {

  /**
   * Retrieves a message by its ID
   *
   * @param messageId message identifier
   * @return The {@link Message} wrapped in an {@link Optional}
   */
  Optional<Message> getById(String messageId);

  /**
   * Retrieves a message by its ID including reactions
   *
   * @param messageId message identifier
   * @return The {@link Message} wrapped in an {@link Optional}
   */
  Optional<Message> getByIdWithReactions(String messageId);

  /**
   * Retrieves messages for a room, paginated by time (MAM support)
   *
   * @param roomId room identifier
   * @param before only messages before this time (null for no upper bound)
   * @param after only messages after this time (null for no lower bound)
   * @param limit maximum number of messages to return
   * @return {@link List} of {@link Message}
   */
  List<Message> getByRoomId(String roomId, OffsetDateTime before, OffsetDateTime after, int limit);

  /**
   * Retrieves messages for a room using a composite cursor (created_at, id) for pagination. This
   * avoids the skip-duplicate bug that occurs when two messages share the same millisecond
   * timestamp and straddle a page boundary.
   *
   * @param roomId room identifier
   * @param before upper timestamp bound (exclusive); combined with beforeId for composite cursor
   * @param beforeId the message ID that anchors the page boundary — used with before to form the
   *     composite cursor {@code (created_at, id) < (before, beforeId)}
   * @param after only messages after this time (null for no lower bound)
   * @param limit maximum number of messages to return
   * @return {@link List} of {@link Message}
   */
  List<Message> getByRoomId(
      String roomId, OffsetDateTime before, String beforeId, OffsetDateTime after, int limit);

  /**
   * Retrieves messages for a room, paginated by time, with configurable sort order
   *
   * @param roomId room identifier
   * @param before only messages before this time (null for no upper bound)
   * @param after only messages after this time (null for no lower bound)
   * @param limit maximum number of messages to return
   * @param ascending true for oldest-first, false for newest-first
   * @return {@link List} of {@link Message}
   */
  List<Message> getByRoomId(
      String roomId, OffsetDateTime before, OffsetDateTime after, int limit, boolean ascending);

  /**
   * Retrieves messages for a room by their ids (batch lookup)
   *
   * @param roomId room identifier
   * @param ids list of message ids to retrieve
   * @return {@link List} of {@link Message}
   */
  List<Message> getByRoomIdAndIds(String roomId, List<String> ids);

  /**
   * Retrieves messages for a room up to a specific message ID (MongooseIM to-id extension). Used
   * for backfilling when navigating to a specific message (e.g., search result).
   *
   * @param roomId room identifier
   * @param toId message ID to load up to (inclusive)
   * @param after only messages after this time (null for no lower bound)
   * @param limit maximum number of messages to return
   * @return {@link List} of {@link Message}
   */
  List<Message> getByRoomIdUpToMessageId(
      String roomId, String toId, OffsetDateTime after, int limit);

  /**
   * Word-AND search in messages for a room: every word must appear (case-insensitively, as a
   * substring, in any order) in the current text.
   *
   * @param roomId room identifier
   * @param words normalized search words (see {@code SearchTextUtils#toSearchWords})
   * @param before only messages before this time (null = no upper bound, returns from most recent)
   * @param after only messages strictly after this time (null = no lower bound); used to hide
   *     history the caller has cleared
   * @param limit maximum number of messages to return
   * @return {@link List} of {@link Message}
   */
  List<Message> searchByText(
      String roomId, List<String> words, OffsetDateTime before, OffsetDateTime after, int limit);

  /**
   * For each room the user is subscribed to, the newest non-deleted message and the newest system
   * event visible to the user (created strictly after the user's cleared history), in one query.
   * Ties on the creation time are resolved by id, like the timeline ordering.
   *
   * @param userId user identifier
   * @return map of roomId to the candidates; rooms without any visible item are absent
   */
  Map<String, InboxLastItemRefs> getLastItemRefsByUserId(String userId);

  /**
   * Unread items per subscribed room: non-deleted messages and system events not caused by the
   * user, newer than the latest of join time, cleared history and read marker. Each count is
   * capped.
   *
   * @param userId user identifier
   * @param cap maximum count returned per room
   * @return map of roomId to unread count (rooms with zero unread are absent)
   */
  Map<String, Long> getUnreadCountsByUserId(String userId, int cap);

  /**
   * Loads the reply targets of the given messages, restricted to those in the same room as the
   * replying message and created strictly after the user's cleared history for that room.
   *
   * @param userId user identifier
   * @param replyingMessageIds identifiers of the replying messages
   * @return {@link List} of the visible original {@link Message}s
   */
  List<Message> getVisibleRepliedOriginals(String userId, Collection<String> replyingMessageIds);

  /**
   * Loads messages by id with their reactions.
   *
   * @param ids message identifiers
   * @return {@link List} of {@link Message}
   */
  List<Message> getByIdsWithReactions(Collection<String> ids);

  /**
   * Forward (oldest→newest) keyset page for bulk export.
   *
   * @param roomId the room
   * @param lowerBoundExclusive if non-null, only messages with createdAt strictly greater than this
   *     are returned (used for the user's clearedAt bound)
   * @param cursorCreatedAt last returned message createdAt, or null for the first page
   * @param cursorId last returned message id, or null for the first page
   * @param limit max rows
   * @return up to {@code limit} messages ordered by (createdAt asc, id asc)
   */
  List<Message> getByRoomIdAfterCursor(
      String roomId,
      OffsetDateTime lowerBoundExclusive,
      OffsetDateTime cursorCreatedAt,
      String cursorId,
      int limit);

  /**
   * Inserts a new message
   *
   * @param message {@link Message} to insert
   * @return {@link Message} inserted
   */
  Message insert(Message message);

  /**
   * Updates a message (for edit/delete)
   *
   * @param message {@link Message} to update
   * @return {@link Message} updated
   */
  Message update(Message message);

  /**
   * Deletes all messages in a room
   *
   * @param roomId room identifier
   */
  void deleteByRoomId(String roomId);
}
