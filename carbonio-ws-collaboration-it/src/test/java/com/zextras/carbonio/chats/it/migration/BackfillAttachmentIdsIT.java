// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.migration.scripts.V1_4_2__backfill_attachment_ids;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class BackfillAttachmentIdsIT {

  @Container
  private static final PostgreSQLContainer<?> wscDb =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("carbonio-ws-collaboration-db")
          .withUsername("carbonio-ws-collaboration-db")
          .withPassword("password");

  @Container
  private static final PostgreSQLContainer<?> messageDispatcherDb =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("carbonio-message-dispatcher-db")
          .withUsername("carbonio-message-dispatcher-db")
          .withPassword("password");

  private static HikariDataSource wscDataSource;

  @BeforeAll
  static void setupSchemas() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(wscDb.getJdbcUrl());
    config.setUsername(wscDb.getUsername());
    config.setPassword(wscDb.getPassword());
    wscDataSource = new HikariDataSource(config);

    Flyway.configure()
        .cleanDisabled(false)
        .locations("classpath:migration")
        .schemas("chats")
        .dataSource(wscDataSource)
        .validateMigrationNaming(true)
        .load()
        .migrate();

    try (Connection conn = messageDispatcherDb.createConnection("")) {
      conn.createStatement()
          .execute(
              """
              CREATE TABLE mam_server_user (
                id        BIGSERIAL PRIMARY KEY,
                server    VARCHAR(250) NOT NULL,
                user_name VARCHAR(250) NOT NULL
              )
              """);
      conn.createStatement()
          .execute(
              """
              CREATE TABLE mam_muc_message (
                id      BIGSERIAL PRIMARY KEY,
                room_id BIGINT NOT NULL REFERENCES mam_server_user(id),
                message BYTEA  NOT NULL
              )
              """);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void cleanUp() throws SQLException {
    try (Connection conn = wscDataSource.getConnection()) {
      conn.createStatement()
          .execute("TRUNCATE chats.file_metadata CASCADE; TRUNCATE chats.room CASCADE;");
    }
    try (Connection conn = messageDispatcherDb.createConnection("")) {
      conn.createStatement().execute("DELETE FROM mam_muc_message; DELETE FROM mam_server_user;");
    }
  }

  private void insertRoom(Connection conn, String roomId) throws SQLException {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO chats.room(id,name,type,created_at,updated_at) VALUES(?,?,?,?,?)")) {
      stmt.setString(1, roomId);
      stmt.setString(2, "Test room");
      stmt.setString(3, "GROUP");
      stmt.setTimestamp(4, Timestamp.from(Instant.now()));
      stmt.setTimestamp(5, Timestamp.from(Instant.now()));
      stmt.executeUpdate();
    }
  }

  private void insertAttachment(Connection conn, String fileId, String roomId, String messageId)
      throws SQLException {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO chats.file_metadata"
                + "(id,name,original_size,mime_type,type,user_id,room_id,message_id,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?)")) {
      stmt.setString(1, fileId);
      stmt.setString(2, "attachment.png");
      stmt.setInt(3, 1024);
      stmt.setString(4, "image/png");
      stmt.setString(5, "ATTACHMENT");
      stmt.setString(6, "user-1");
      stmt.setString(7, roomId);
      stmt.setString(8, messageId);
      stmt.setTimestamp(9, Timestamp.from(Instant.now()));
      stmt.setTimestamp(10, Timestamp.from(Instant.now()));
      stmt.executeUpdate();
    }
  }

  private long insertMamRoom(Connection conn, String roomId) throws SQLException {
    try (PreparedStatement stmt =
            conn.prepareStatement(
                "INSERT INTO mam_server_user(server,user_name) VALUES(?,?) RETURNING id");
        ResultSet rs =
            executeReturning(
                stmt,
                ps -> {
                  ps.setString(1, "muclight.carbonio");
                  ps.setString(2, roomId);
                })) {
      rs.next();
      return rs.getLong(1);
    }
  }

  private long insertMamMessage(Connection conn, long mamRoomId, String fileId, String msgId)
      throws SQLException {
    String xml =
        "<message id='%s'><attachment-id>%s</attachment-id></message>".formatted(msgId, fileId);
    try (PreparedStatement stmt =
            conn.prepareStatement(
                "INSERT INTO mam_muc_message(room_id,message) VALUES(?,?) RETURNING id");
        ResultSet rs =
            executeReturning(
                stmt,
                ps -> {
                  ps.setLong(1, mamRoomId);
                  ps.setBytes(2, xml.getBytes());
                })) {
      rs.next();
      return rs.getLong(1);
    }
  }

  @FunctionalInterface
  interface StatementBinder {
    void bind(PreparedStatement stmt) throws SQLException;
  }

  private ResultSet executeReturning(PreparedStatement stmt, StatementBinder binder)
      throws SQLException {
    binder.bind(stmt);
    return stmt.executeQuery();
  }

  private record FileMetadataRow(String stanzaId, String messageId) {}

  private FileMetadataRow readMetadata(String fileId) throws SQLException {
    try (Connection conn = wscDataSource.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT stanza_id, message_id FROM chats.file_metadata WHERE id = ?")) {
      stmt.setString(1, fileId);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return new FileMetadataRow(rs.getString(1), rs.getString(2));
      }
    }
  }

  private void runMigration() throws Exception {
    MessageDispatcherCredentials credentials =
        new MessageDispatcherCredentials(
            messageDispatcherDb.getHost(),
            messageDispatcherDb.getMappedPort(5432),
            messageDispatcherDb.getDatabaseName(),
            messageDispatcherDb.getUsername(),
            messageDispatcherDb.getPassword());
    V1_4_2__backfill_attachment_ids migration = new V1_4_2__backfill_attachment_ids(credentials);
    try (Connection wscConn = wscDataSource.getConnection()) {
      migration.migrate(
          new Context() {
            @Override
            public Configuration getConfiguration() {
              return null;
            }

            @Override
            public Connection getConnection() {
              return wscConn;
            }
          });
    }
  }

  @Test
  @DisplayName("stanza_id and message_id are backfilled from MAM archive")
  void backfillsStanzaIdAndMessageId() throws Exception {
    String roomId = "room-1";
    String fileId = "file-1";
    String msgId = "msg-uuid-1";

    try (Connection wsc = wscDataSource.getConnection();
        Connection messageDispatcher = messageDispatcherDb.createConnection("")) {
      insertRoom(wsc, roomId);
      insertAttachment(wsc, fileId, roomId, null);
      long mamRoomId = insertMamRoom(messageDispatcher, roomId);
      long mamMsgId = insertMamMessage(messageDispatcher, mamRoomId, fileId, msgId);

      runMigration();

      FileMetadataRow row = readMetadata(fileId);
      assertEquals(V1_4_2__backfill_attachment_ids.toErlangBase32(mamMsgId), row.stanzaId());
      assertEquals(msgId, row.messageId());
    }
  }

  @Test
  @DisplayName("attachment not in MAM is left unchanged")
  void leavesUnresolvedAttachmentUnchanged() throws Exception {
    String roomId = "room-2";
    String fileId = "file-orphan";

    try (Connection wsc = wscDataSource.getConnection();
        Connection messageDispatcher = messageDispatcherDb.createConnection("")) {
      insertRoom(wsc, roomId);
      insertAttachment(wsc, fileId, roomId, null);
      insertMamRoom(messageDispatcher, roomId);
      // no mam_muc_message for this file

      runMigration();

      FileMetadataRow row = readMetadata(fileId);
      assertNull(row.stanzaId());
      assertNull(row.messageId());
    }
  }

  @Test
  @DisplayName("existing message_id is preserved (COALESCE semantics)")
  void preservesExistingMessageId() throws Exception {
    String roomId = "room-3";
    String fileId = "file-2";
    String existingMsgId = "existing-msg-id";
    String mamMsgId = "mam-msg-id";

    try (Connection wsc = wscDataSource.getConnection();
        Connection messageDispatcher = messageDispatcherDb.createConnection("")) {
      insertRoom(wsc, roomId);
      insertAttachment(wsc, fileId, roomId, existingMsgId);
      long mamRoomId = insertMamRoom(messageDispatcher, roomId);
      insertMamMessage(messageDispatcher, mamRoomId, fileId, mamMsgId);

      runMigration();

      FileMetadataRow row = readMetadata(fileId);
      assertNotNull(row.stanzaId());
      assertEquals(existingMsgId, row.messageId());
    }
  }

  @Test
  @DisplayName("skips gracefully when credentials are unavailable")
  void skipsWhenCredentialsUnavailable() throws Exception {
    String roomId = "room-4";
    String fileId = "file-3";

    try (Connection wsc = wscDataSource.getConnection()) {
      insertRoom(wsc, roomId);
      insertAttachment(wsc, fileId, roomId, null);
    }

    MessageDispatcherCredentials noCredentials =
        new MessageDispatcherCredentials("localhost", 5432, null, null, null);
    V1_4_2__backfill_attachment_ids migration = new V1_4_2__backfill_attachment_ids(noCredentials);

    try (Connection wscConn = wscDataSource.getConnection()) {
      migration.migrate(
          new Context() {
            @Override
            public Configuration getConfiguration() {
              return null;
            }

            @Override
            public Connection getConnection() {
              return wscConn;
            }
          });
    }

    FileMetadataRow row = readMetadata(fileId);
    assertNull(row.stanzaId());
    assertNull(row.messageId());
  }

  @Test
  @DisplayName("1050 attachments across 10 rooms are all backfilled, 50 orphans left unchanged")
  void backfillsLargeDataset() throws Exception {
    int roomCount = 10;
    int perRoom = 105; // 1050 resolved total — crosses BATCH_SIZE=500 twice
    int orphanedCount = 50;

    try (Connection wsc = wscDataSource.getConnection();
        Connection messageDispatcher = messageDispatcherDb.createConnection("")) {
      wsc.setAutoCommit(false);
      messageDispatcher.setAutoCommit(false);

      for (int r = 0; r < roomCount; r++) {
        String roomId = "bulk-room-" + r;
        insertRoom(wsc, roomId);
        long mamRoomId = insertMamRoom(messageDispatcher, roomId);

        try (PreparedStatement wscBatch =
                wsc.prepareStatement(
                    "INSERT INTO chats.file_metadata"
                        + "(id,name,original_size,mime_type,type,user_id,room_id,created_at,updated_at)"
                        + " VALUES(?,?,?,?,?,?,?,?,?)");
            PreparedStatement dispBatch =
                messageDispatcher.prepareStatement(
                    "INSERT INTO mam_muc_message(room_id,message) VALUES(?,?)")) {

          for (int a = 0; a < perRoom; a++) {
            String fileId = "bulk-r%d-a%d".formatted(r, a);
            String xml =
                "<message id='msg-%s'><attachment-id>%s</attachment-id></message>"
                    .formatted(fileId, fileId);

            wscBatch.setString(1, fileId);
            wscBatch.setString(2, "file.png");
            wscBatch.setInt(3, 1024);
            wscBatch.setString(4, "image/png");
            wscBatch.setString(5, "ATTACHMENT");
            wscBatch.setString(6, "user-bulk");
            wscBatch.setString(7, roomId);
            wscBatch.setTimestamp(8, Timestamp.from(Instant.now()));
            wscBatch.setTimestamp(9, Timestamp.from(Instant.now()));
            wscBatch.addBatch();

            dispBatch.setLong(1, mamRoomId);
            dispBatch.setBytes(2, xml.getBytes());
            dispBatch.addBatch();
          }
          wscBatch.executeBatch();
          dispBatch.executeBatch();
        }
      }

      String orphanRoom = "bulk-orphan-room";
      insertRoom(wsc, orphanRoom);
      insertMamRoom(messageDispatcher, orphanRoom);

      try (PreparedStatement stmt =
          wsc.prepareStatement(
              "INSERT INTO chats.file_metadata"
                  + "(id,name,original_size,mime_type,type,user_id,room_id,created_at,updated_at)"
                  + " VALUES(?,?,?,?,?,?,?,?,?)")) {
        for (int i = 0; i < orphanedCount; i++) {
          stmt.setString(1, "bulk-orphan-" + i);
          stmt.setString(2, "orphan.png");
          stmt.setInt(3, 1024);
          stmt.setString(4, "image/png");
          stmt.setString(5, "ATTACHMENT");
          stmt.setString(6, "user-bulk");
          stmt.setString(7, orphanRoom);
          stmt.setTimestamp(8, Timestamp.from(Instant.now()));
          stmt.setTimestamp(9, Timestamp.from(Instant.now()));
          stmt.addBatch();
        }
        stmt.executeBatch();
      }

      wsc.commit();
      messageDispatcher.commit();
    }

    runMigration();

    try (Connection conn = wscDataSource.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "SELECT COUNT(*) FROM chats.file_metadata WHERE stanza_id IS NOT NULL")) {
        try (ResultSet rs = stmt.executeQuery()) {
          rs.next();
          assertEquals(roomCount * perRoom, rs.getLong(1));
        }
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "SELECT COUNT(*) FROM chats.file_metadata WHERE stanza_id IS NULL AND room_id IS NOT"
                  + " NULL")) {
        try (ResultSet rs = stmt.executeQuery()) {
          rs.next();
          assertEquals(orphanedCount, rs.getLong(1));
        }
      }
    }

    FileMetadataRow sample = readMetadata("bulk-r0-a0");
    assertNotNull(sample.stanzaId());
    assertEquals("msg-bulk-r0-a0", sample.messageId());

    List.of("bulk-orphan-0", "bulk-orphan-" + (orphanedCount - 1))
        .forEach(
            id -> {
              try {
                assertNull(readMetadata(id).stanzaId());
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  @DisplayName("multiple attachments in the same room are all backfilled")
  void backfillsMultipleAttachmentsInSameRoom() throws Exception {
    String roomId = "room-5";
    String fileId1 = "file-4";
    String fileId2 = "file-5";
    String msgId1 = "msg-uuid-4";
    String msgId2 = "msg-uuid-5";

    try (Connection wsc = wscDataSource.getConnection();
        Connection messageDispatcher = messageDispatcherDb.createConnection("")) {
      insertRoom(wsc, roomId);
      insertAttachment(wsc, fileId1, roomId, null);
      insertAttachment(wsc, fileId2, roomId, null);
      long mamRoomId = insertMamRoom(messageDispatcher, roomId);
      long mamMsgId1 = insertMamMessage(messageDispatcher, mamRoomId, fileId1, msgId1);
      long mamMsgId2 = insertMamMessage(messageDispatcher, mamRoomId, fileId2, msgId2);

      runMigration();

      FileMetadataRow row1 = readMetadata(fileId1);
      assertEquals(V1_4_2__backfill_attachment_ids.toErlangBase32(mamMsgId1), row1.stanzaId());
      assertEquals(msgId1, row1.messageId());

      FileMetadataRow row2 = readMetadata(fileId2);
      assertEquals(V1_4_2__backfill_attachment_ids.toErlangBase32(mamMsgId2), row2.stanzaId());
      assertEquals(msgId2, row2.messageId());
    }
  }
}
