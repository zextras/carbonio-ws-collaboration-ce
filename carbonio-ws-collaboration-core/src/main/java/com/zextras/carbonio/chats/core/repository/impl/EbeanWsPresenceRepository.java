// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.UserLastSeen;
import com.zextras.carbonio.chats.core.data.entity.WsSession;
import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import io.ebean.Database;
import io.ebean.SqlRow;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
public class EbeanWsPresenceRepository implements WsPresenceRepository {

  private static final String UPSERT_LAST_SEEN_SQL =
      "INSERT INTO chats.user_last_seen (user_id, last_seen_at)"
          + " VALUES (:userId, NOW())"
          + " ON CONFLICT (user_id) DO UPDATE SET last_seen_at = NOW()"
          + " RETURNING last_seen_at";

  // Data-modifying CTE: the INSERT runs inside the CTE, while the outer SELECT count(*) reads the
  // PRE-insert snapshot of ws_session. A returned count of 0 therefore means this is the user's
  // first session (the 0 -> 1 online transition). One round-trip instead of INSERT + COUNT.
  private static final String INSERT_AND_COUNT_SQL =
      """
      WITH ins AS (
        INSERT INTO chats.ws_session (session_id, user_id, service_id)
        VALUES (:sessionId, :userId, :serviceId)
        ON CONFLICT (session_id) DO NOTHING
        RETURNING user_id
      )
      SELECT count(*) AS pre_count
      FROM chats.ws_session
      WHERE user_id = :userId
      """;

  // Data-modifying CTE: the DELETE runs inside the CTE, while the outer SELECT count(*) reads the
  // PRE-delete snapshot (which still includes the row being deleted). A returned count of 1
  // therefore means this was the user's last session (the 1 -> 0 offline transition). When no row
  // matches, del is empty so deleted_user_id is NULL and the caller treats it as a no-op.
  // One round-trip instead of DELETE + COUNT.
  private static final String DELETE_AND_COUNT_SQL =
      """
      WITH del AS (
        DELETE FROM chats.ws_session
        WHERE session_id = :sessionId
        RETURNING user_id
      )
      SELECT (SELECT user_id FROM del) AS deleted_user_id,
             (SELECT count(*) FROM chats.ws_session s
              WHERE s.user_id = (SELECT user_id FROM del)) AS pre_count
      """;

  // Set-based crash recovery, all in ONE statement:
  //   1. removed   - delete every row for the stale service, RETURNING the affected user ids;
  //   2. offlined  - of those, the DISTINCT users with zero remaining sessions anywhere
  //                  (anti-join against the POST-delete snapshot of ws_session);
  //   3. upserted  - one batched INSERT ... ON CONFLICT bumping last_seen_at for exactly those
  //                  offlined users.
  // The outer SELECT returns the offlined user ids with their new last_seen_at for the broadcast.
  // O(1) SQL round-trips regardless of how many users/sessions are affected (replaces the previous
  // per-user COUNT(*) + per-user upsert N+1 loop).
  private static final String DELETE_BY_SERVICE_RECOVER_SQL =
      """
      WITH removed AS (
        DELETE FROM chats.ws_session
        WHERE service_id = :serviceId
        RETURNING user_id
      ),
      offlined AS (
        SELECT DISTINCT r.user_id AS user_id
        FROM removed r
        WHERE NOT EXISTS (
          SELECT 1 FROM chats.ws_session s WHERE s.user_id = r.user_id AND s.service_id <> :serviceId
        )
      ),
      upserted AS (
        INSERT INTO chats.user_last_seen (user_id, last_seen_at)
        SELECT user_id, NOW() FROM offlined
        ON CONFLICT (user_id) DO UPDATE SET last_seen_at = NOW()
        RETURNING user_id, last_seen_at
      )
      SELECT user_id, last_seen_at FROM upserted
      """;

  private static final String BATCH_PRESENCE_SQL =
      """
      SELECT u.user_id,
             (COUNT(s.session_id) > 0) AS online,
             ls.last_seen_at
      FROM UNNEST(STRING_TO_ARRAY(:userIds, ',')) AS u(user_id)
      LEFT JOIN chats.ws_session s ON s.user_id = u.user_id
      LEFT JOIN chats.user_last_seen ls ON ls.user_id = u.user_id
      GROUP BY u.user_id, ls.last_seen_at
      """;

  private final Database db;

  @Inject
  public EbeanWsPresenceRepository(Database db) {
    this.db = db;
  }

  @Override
  public long insertAndCountOnline(String sessionId, String userId, String serviceId) {
    Long preCount =
        db.sqlQuery(INSERT_AND_COUNT_SQL)
            .setParameter("sessionId", sessionId)
            .setParameter("userId", userId)
            .setParameter("serviceId", serviceId)
            .mapToScalar(Long.class)
            .findOne();
    return preCount != null ? preCount : 0L;
  }

  @Override
  public SessionDeletionResult deleteAndCountRemaining(String sessionId) {
    SqlRow row = db.sqlQuery(DELETE_AND_COUNT_SQL).setParameter("sessionId", sessionId).findOne();
    if (row == null) {
      // count(*) always yields a row; defensive only.
      return new SessionDeletionResult(null, 0L);
    }
    String deletedUserId = row.getString("deleted_user_id");
    Long preCount = row.getLong("pre_count");
    return new SessionDeletionResult(deletedUserId, preCount != null ? preCount : 0L);
  }

  @Override
  public int countOnlineByUserId(String userId) {
    return db.find(WsSession.class).where().eq("userId", userId).findCount();
  }

  @Override
  public OffsetDateTime upsertLastSeen(String userId) {
    return toUtc(
        db.sqlQuery(UPSERT_LAST_SEEN_SQL)
            .setParameter("userId", userId)
            .findOne()
            .getTimestamp("last_seen_at"));
  }

  @Override
  public Optional<OffsetDateTime> findLastSeenForUser(String userId) {
    return Optional.ofNullable(db.find(UserLastSeen.class, userId))
        .map(UserLastSeen::getLastSeenAt);
  }

  @Override
  public Map<String, OffsetDateTime> recoverStaleService(String serviceId) {
    Map<String, OffsetDateTime> offlined = new LinkedHashMap<>();
    db.sqlQuery(DELETE_BY_SERVICE_RECOVER_SQL)
        .setParameter("serviceId", serviceId)
        .findEachRow(
            (row, rowNum) ->
                offlined.put(row.getString("user_id"), toUtc(row.getTimestamp("last_seen_at"))));
    return offlined;
  }

  @Override
  public Map<String, String> findSessionsByServiceId(String serviceId) {
    Map<String, String> sessions = new HashMap<>();
    db.find(WsSession.class)
        .select("sessionId, userId")
        .where()
        .eq("serviceId", serviceId)
        .findEach(s -> sessions.put(s.getSessionId(), s.getUserId()));
    return sessions;
  }

  @Override
  public Set<String> findDistinctOnlineServiceIds() {
    List<String> list =
        db.find(WsSession.class).select("serviceId").setDistinct(true).findSingleAttributeList();
    return new HashSet<>(list);
  }

  @Override
  public List<PresenceSummary> getBatchPresence(Collection<String> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return List.of();
    }

    Set<String> remaining = new HashSet<>(userIds);
    List<PresenceSummary> results = new ArrayList<>();

    db.sqlQuery(BATCH_PRESENCE_SQL)
        .setParameter("userIds", String.join(",", userIds))
        .findEachRow(
            (row, rowNum) -> {
              String userId = row.getString("user_id");
              boolean online = Boolean.TRUE.equals(row.getBoolean("online"));
              Timestamp ts = row.getTimestamp("last_seen_at");
              OffsetDateTime lastSeen = ts != null ? toUtc(ts) : null;
              results.add(new PresenceSummary(userId, online, lastSeen));
              remaining.remove(userId);
            });

    // Add offline entries for any user not returned by the query
    for (String userId : remaining) {
      results.add(new PresenceSummary(userId, false, null));
    }

    return results;
  }

  private static OffsetDateTime toUtc(Timestamp timestamp) {
    return timestamp.toInstant().atOffset(ZoneOffset.UTC);
  }
}
