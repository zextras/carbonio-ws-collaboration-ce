// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface WsPresenceRepository {

  /**
   * Inserts a new online WebSocket session record and returns the number of sessions the user had
   * <em>before</em> this insert, in a single round-trip.
   *
   * <p>Implemented as one data-modifying CTE: the {@code INSERT ... RETURNING} runs inside the CTE
   * while the outer {@code SELECT count(*)} sees the PRE-insert snapshot of {@code WS_SESSION}.
   * Therefore a returned count of {@code 0} means this insert is the user's {@code 0 -> 1}
   * transition (the user just came online cluster-wide).
   *
   * <p>Idempotent: an existing session row is left untouched and counted, so no transition.
   *
   * @param sessionId WebSocket session identifier
   * @param userId user identifier
   * @param serviceId service/node identifier
   * @return the number of active sessions the user had before this insert ({@code 0} == this is the
   *     first session, i.e. the online transition)
   */
  long insertAndCountOnline(String sessionId, String userId, String serviceId);

  /**
   * Deletes a WebSocket session by its session identifier and returns, in a single round-trip, the
   * number of sessions that existed for the affected user <em>before</em> this delete.
   *
   * <p>Implemented as one data-modifying CTE: the {@code DELETE ... RETURNING user_id} runs inside
   * the CTE while the outer {@code SELECT count(*)} sees the PRE-delete snapshot (which still
   * includes the row being deleted). Therefore a returned count of {@code 1} means that after the
   * delete the user has {@code 0} sessions, i.e. this is the user's {@code 1 -> 0} transition (the
   * user just went offline cluster-wide).
   *
   * <p>If the session did not exist, no row is deleted: the returned {@link
   * SessionDeletionResult#userId()} is {@code null} and the caller must treat the operation as a
   * no-op (no transition).
   *
   * @param sessionId WebSocket session identifier
   * @return a {@link SessionDeletionResult} carrying the deleted user id (or {@code null} if the
   *     session was not found) and the pre-delete session count for that user
   */
  SessionDeletionResult deleteAndCountRemaining(String sessionId);

  /**
   * Result of {@link #deleteAndCountRemaining(String)}.
   *
   * @param userId the user id of the deleted session, or {@code null} if no session matched
   * @param preDeleteCount the number of sessions the user had before the delete ({@code 1} == this
   *     was the last session, i.e. the offline transition); {@code 0} when nothing was deleted
   */
  record SessionDeletionResult(String userId, long preDeleteCount) {

    /**
     * @return {@code true} when a session row was actually deleted
     */
    public boolean deleted() {
      return userId != null;
    }

    /**
     * @return {@code true} when this delete was the user's last session (offline transition)
     */
    public boolean wasLastSession() {
      return deleted() && preDeleteCount == 1;
    }
  }

  /**
   * Counts the number of active sessions for a given user across all nodes.
   *
   * @param userId user identifier
   * @return number of active sessions
   */
  int countOnlineByUserId(String userId);

  /**
   * Inserts or updates the last-seen timestamp for a user to the current time.
   *
   * @param userId user identifier
   * @return the stored last-seen timestamp
   */
  OffsetDateTime upsertLastSeen(String userId);

  /**
   * Returns the last-seen timestamp for a user, if present.
   *
   * @param userId user identifier
   * @return the last-seen {@link OffsetDateTime} wrapped in an {@link Optional}
   */
  Optional<OffsetDateTime> findLastSeenForUser(String userId);

  /**
   * Recovers presence for a stale (crashed / never-returned) service/node in a single set-based
   * round-trip: deletes all of the service's {@code WS_SESSION} rows, bumps {@code USER_LAST_SEEN}
   * for the users that drop to zero sessions, and returns those users so the caller can broadcast
   * OFFLINE.
   *
   * <p>Implemented as one data-modifying CTE:
   *
   * <ol>
   *   <li>{@code removed} — {@code DELETE ... RETURNING user_id} removes the service's rows;
   *   <li>{@code offlined} — a {@code NOT EXISTS} anti-join against the post-delete snapshot of
   *       {@code WS_SESSION} selects the distinct affected users who are now fully offline;
   *   <li>{@code upserted} — one batched {@code INSERT ... ON CONFLICT} bumps {@code last_seen_at}
   *       for exactly those users.
   * </ol>
   *
   * <p>This replaces the previous per-affected-user {@code COUNT(*)} + {@code upsert} loop (N+1)
   * with O(1) SQL round-trips regardless of the number of affected users.
   *
   * @param serviceId service/node identifier
   * @return the users whose total session count dropped to zero (now offline), each with the {@code
   *     last_seen_at} already upserted for them
   */
  Map<String, OffsetDateTime> recoverStaleService(String serviceId);

  Map<String, String> findSessionsByServiceId(String serviceId);

  /**
   * Returns the distinct set of service/node identifiers that have at least one active session.
   *
   * @return set of service identifiers
   */
  Set<String> findDistinctOnlineServiceIds();

  /**
   * Returns presence summaries for a batch of user identifiers. Users not found in the result are
   * included as offline with a {@code null} last-seen timestamp.
   *
   * @param userIds collection of user identifiers to query
   * @return list of {@link PresenceSummary}
   */
  List<PresenceSummary> getBatchPresence(Collection<String> userIds);
}
