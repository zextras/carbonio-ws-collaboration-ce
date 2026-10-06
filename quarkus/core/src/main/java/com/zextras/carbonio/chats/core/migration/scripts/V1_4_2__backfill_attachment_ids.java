// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.scripts;

import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Backfills {@code stanza_id} and {@code message_id} for {@code FILE_METADATA} ATTACHMENT records
 * that were created before the message-dispatcher PR #3 patch. For each record, the migration
 * locates the archived XMPP stanza in the message-dispatcher MAM database, derives {@code
 * stanza_id} by encoding the MAM message BIGINT as Erlang base-32, and extracts {@code message_id}
 * from the {@code <message id="...">} attribute. {@code message_id} is only written when the column
 * is currently {@code NULL} (COALESCE semantics).
 *
 * <p>The migration is skipped gracefully when the message-dispatcher database credentials are
 * unavailable (fresh installs, test environments). Attachment records whose stanza cannot be found
 * in the MAM archive (e.g. purged rooms) are logged and left unchanged.
 */
public class V1_4_2__backfill_attachment_ids extends BaseJavaMigration {

  private static final String LOG_PREFIX = "[Attachment message ids backfill]";
  private static final String BASE32_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUV";
  private static final String MUC_DOMAIN = "muclight.carbonio";
  private static final int BATCH_SIZE = 500;

  private final MessageDispatcherCredentials credentials;

  public V1_4_2__backfill_attachment_ids(MessageDispatcherCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public void migrate(Context context) throws Exception {
    if (!credentials.isAvailable()) {
      ChatsLogger.warn(LOG_PREFIX + " message-dispatcher DB credentials not available — skipping");
      return;
    }

    try (Connection dispatcherConn =
        openMessageDispatcherConnection(
            credentials.getJdbcUrl(), credentials.getUsername(), credentials.getPassword())) {
      if (dispatcherConn == null) {
        ChatsLogger.warn(LOG_PREFIX + " Could not connect to message-dispatcher DB — skipping");
        return;
      }
      backfill(context.getConnection(), dispatcherConn);
    }
  }

  private void backfill(Connection chatsConn, Connection dispatcherConn) throws Exception {
    List<AttachmentRow> rows = fetchUnresolved(chatsConn);
    ChatsLogger.info(
        LOG_PREFIX + " " + rows.size() + " attachments with stanza_id and message_id = NULL");

    if (rows.isEmpty()) {
      return;
    }

    // Group by room: one dispatcher query per room instead of one per attachment
    Map<String, List<AttachmentRow>> byRoom =
        rows.stream().collect(Collectors.groupingBy(AttachmentRow::roomId));

    int resolved = 0;
    int notFound = 0;
    List<UpdateRow> batch = new ArrayList<>(BATCH_SIZE);

    for (Map.Entry<String, List<AttachmentRow>> entry : byRoom.entrySet()) {
      Set<String> fileIds =
          entry.getValue().stream().map(AttachmentRow::fileId).collect(Collectors.toSet());
      Map<String, MamRecord> mamRecords =
          findMamRecordsForRoom(dispatcherConn, entry.getKey(), fileIds);

      for (AttachmentRow row : entry.getValue()) {
        MamRecord mam = mamRecords.get(row.fileId());
        if (mam != null) {
          batch.add(new UpdateRow(mam.stanzaId(), mam.messageId(), row.fileId()));
          resolved++;
        } else {
          ChatsLogger.warn(
              LOG_PREFIX + " file_id=" + row.fileId() + " not found in MAM (likely purged)");
          notFound++;
        }

        if (batch.size() >= BATCH_SIZE) {
          applyBatch(chatsConn, batch);
          batch.clear();
        }
      }
    }

    if (!batch.isEmpty()) {
      applyBatch(chatsConn, batch);
    }

    ChatsLogger.info(
        LOG_PREFIX + " Completed: " + resolved + " resolved, " + notFound + " not found in MAM");
  }

  private List<AttachmentRow> fetchUnresolved(Connection chatsConn) throws SQLException {
    String sql =
        "SELECT room_id, id, message_id FROM chats.file_metadata"
            + " WHERE type = 'ATTACHMENT' AND stanza_id IS NULL AND room_id IS NOT NULL"
            + " ORDER BY room_id";
    List<AttachmentRow> rows = new ArrayList<>();
    try (PreparedStatement stmt = chatsConn.prepareStatement(sql)) {
      stmt.setFetchSize(BATCH_SIZE);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          rows.add(new AttachmentRow(rs.getString(1), rs.getString(2), rs.getString(3)));
        }
      }
    }
    return rows;
  }

  private Map<String, MamRecord> findMamRecordsForRoom(
      Connection dispatcherConn, String roomId, Set<String> fileIds) {
    String sql =
        "SELECT m.id, convert_from(m.message, 'UTF8')"
            + " FROM mam_muc_message m"
            + " JOIN mam_server_user msu ON m.room_id = msu.id"
            + " WHERE msu.server = ? AND msu.user_name = ?"
            + " AND convert_from(m.message, 'UTF8') LIKE '%<attachment-id>%'"
            + " ORDER BY m.id";
    Map<String, MamRecord> result = new HashMap<>(fileIds.size());
    try (PreparedStatement stmt = dispatcherConn.prepareStatement(sql)) {
      stmt.setFetchSize(BATCH_SIZE);
      stmt.setString(1, MUC_DOMAIN);
      stmt.setString(2, roomId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next() && result.size() < fileIds.size()) {
          String xml = rs.getString(2);
          String attachmentId = extractAttachmentId(xml);
          if (attachmentId != null
              && fileIds.contains(attachmentId)
              && !result.containsKey(attachmentId)) {
            result.put(
                attachmentId, new MamRecord(toErlangBase32(rs.getLong(1)), extractMessageId(xml)));
          }
        }
      }
    } catch (SQLException e) {
      ChatsLogger.warn(
          LOG_PREFIX + " MAM lookup failed for room=" + roomId + ": " + e.getMessage());
    }
    return result;
  }

  private static String extractAttachmentId(String xml) {
    int start = xml.indexOf("<attachment-id>");
    if (start < 0) return null;
    start += "<attachment-id>".length();
    int end = xml.indexOf("</attachment-id>", start);
    return end > start ? xml.substring(start, end) : null;
  }

  private void applyBatch(Connection chatsConn, List<UpdateRow> updates) throws SQLException {
    String sql =
        "UPDATE chats.file_metadata"
            + " SET stanza_id = ?, message_id = COALESCE(message_id, ?)"
            + " WHERE id = ?";
    try (PreparedStatement stmt = chatsConn.prepareStatement(sql)) {
      for (UpdateRow u : updates) {
        stmt.setString(1, u.stanzaId());
        stmt.setString(2, u.messageId());
        stmt.setString(3, u.fileId());
        stmt.addBatch();
      }
      stmt.executeBatch();
    }
  }

  public static String toErlangBase32(long n) {
    if (n == 0) return "0";
    StringBuilder sb = new StringBuilder();
    long remaining = n;
    while (remaining != 0) {
      sb.append(BASE32_CHARS.charAt((int) (remaining % 32)));
      remaining /= 32;
    }
    return sb.reverse().toString();
  }

  private static String extractMessageId(String xml) {
    int tagEnd = xml.indexOf('>');
    String openTag = tagEnd > 0 ? xml.substring(0, tagEnd) : xml;
    int idIdx = openTag.indexOf(" id=\"");
    if (idIdx >= 0) {
      int start = idIdx + 5;
      int end = openTag.indexOf('"', start);
      return end > start ? openTag.substring(start, end) : null;
    }
    idIdx = openTag.indexOf(" id='");
    if (idIdx >= 0) {
      int start = idIdx + 5;
      int end = openTag.indexOf('\'', start);
      return end > start ? openTag.substring(start, end) : null;
    }
    return null;
  }

  private Connection openMessageDispatcherConnection(String jdbcUrl, String user, String pass) {
    try {
      Properties props = new Properties();
      props.setProperty("user", user);
      props.setProperty("password", pass);
      props.setProperty("sslmode", "disable");
      return DriverManager.getConnection(jdbcUrl, props);
    } catch (SQLException e) {
      ChatsLogger.warn(LOG_PREFIX + " Cannot connect to message-dispatcher DB: " + e.getMessage());
      return null;
    }
  }

  private record AttachmentRow(String roomId, String fileId, String messageId) {}

  private record MamRecord(String stanzaId, String messageId) {}

  private record UpdateRow(String stanzaId, String messageId, String fileId) {}
}
