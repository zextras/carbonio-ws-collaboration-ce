// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.migration.mongooseimport.ParsedRow;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ParsedRow.Kind;
import com.zextras.carbonio.chats.core.migration.mongooseimport.StanzaIds;
import com.zextras.carbonio.chats.core.migration.mongooseimport.StanzaParser;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import: references to rows older than the in-memory room window")
class ReferencesBeyondWindowIT {

  private static final String ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final int WINDOW = 8192;
  private static final int FILLERS = 8500;

  private static final long LAST_ROW_BEFORE_FILLERS = 458596925311798017L;
  private static final long LAST_ROW_BEFORE_TARGET = 458596925209478145L;
  private static final long TARGET =
      (((LAST_ROW_BEFORE_TARGET >> 8) + 1) << 8) | (LAST_ROW_BEFORE_TARGET & 255);
  private static final long DELETED_TARGET = 458596907231357953L;
  private static final long TEMPLATE_TEXT = 458596911276023041L;
  private static final long TEMPLATE_REPLY = 458596912718090497L;
  private static final String TEMPLATE_REPLY_TARGET = "CNA3EJUJV581";

  private static final String FILLER_SENDER = "97c353af-4374-5ff3-a3eb-3056c0b4dba9";
  private static final String OTHER_SENDER_1 = "7d03b0c9-cd6e-5392-9186-1b9646f07fef";
  private static final String OTHER_SENDER_2 = "7d7ebbe1-c1c7-513f-bae9-24131c672d1d";
  private static final String OTHER_SENDER_3 = "f65a7fdc-542e-53bf-9722-5e580cdcca42";
  private static final long MARKED_FIRST = 458596910664776705L;
  private static final long MARKED_AT_MARKER = 458596910764379393L;
  private static final long MARKED_LATER = 458596910865479937L;
  private static final String MARKED_CLIENT_ID = "4ddba31f-4d76-4fe9-b6c3-0d8c0c3db8dd";
  private static final String MARKER_USER = "a793945e-d600-5362-8ecf-bb6b1d88dbc3";

  private static final Pattern UUID_PATTERN =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

  private record SourceRow(long mam, String sender, ParsedRow parsed) {}

  private static ImportedChats plain;
  private static ImportedChats padded;
  private static List<SourceRow> source;
  private static Set<String> excludedIds;
  private static long edgeReplyMam;
  private static long replyToTargetMam;
  private static String pinStanza;

  private static long fillerMam(int k) {
    return (((LAST_ROW_BEFORE_FILLERS >> 8) + k) << 8) | (LAST_ROW_BEFORE_FILLERS & 255);
  }

  @BeforeAll
  static void importPlainAndPadded() throws SQLException {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    ImportRun unmodified = MongooseImport.ofDump();
    assertTrue(unmodified.succeeded(), unmodified::failureText);
    plain = unmodified.chats();

    String sourceDatabase = database.newSourceDatabase();
    String roomKey =
        "(SELECT id FROM mam_server_user WHERE server = 'muclight.carbonio' AND user_name = '"
            + ROOM
            + "')";
    String clientId = "substring(convert_from(t.message, 'UTF8') from ' id=''([^'']*)''')";
    edgeReplyMam = fillerMam(FILLERS + 1);
    replyToTargetMam = fillerMam(FILLERS + 9);
    database.execute(
        sourceDatabase,
        "INSERT INTO mam_muc_message (id, room_id, sender_id, nick_name, message, search_body,"
            + " origin_id) SELECT (("
            + (LAST_ROW_BEFORE_FILLERS >> 8)
            + " + g) << 8) | "
            + (LAST_ROW_BEFORE_FILLERS & 255)
            + ", t.room_id, t.sender_id, t.nick_name, convert_to(replace(convert_from(t.message,"
            + " 'UTF8'), "
            + clientId
            + ", 'filler-' || g), 'UTF8'), t.search_body, NULL FROM mam_muc_message t,"
            + " generate_series(1, "
            + FILLERS
            + ") g WHERE t.room_id = "
            + roomKey
            + " AND t.id = "
            + TEMPLATE_TEXT,
        "INSERT INTO mam_muc_message (id, room_id, sender_id, nick_name, message, search_body,"
            + " origin_id) SELECT "
            + edgeReplyMam
            + ", t.room_id, t.sender_id, t.nick_name, convert_to(replace(replace(convert_from("
            + "t.message, 'UTF8'), "
            + clientId
            + ", 'filler-reply'), '''"
            + TEMPLATE_REPLY_TARGET
            + "''', '''"
            + StanzaIds.encode(LAST_ROW_BEFORE_FILLERS)
            + "'''), 'UTF8'), t.search_body, NULL FROM mam_muc_message t WHERE t.room_id = "
            + roomKey
            + " AND t.id = "
            + TEMPLATE_REPLY,
        raw(TARGET, FILLER_SENDER, text("filler-target", "target before the window")),
        raw(fillerMam(FILLERS + 2), FILLER_SENDER, edit("filler-edit-1", TARGET, "edited once")),
        raw(fillerMam(FILLERS + 3), FILLER_SENDER, edit("filler-edit-2", TARGET, "edited twice")),
        raw(
            fillerMam(FILLERS + 4),
            OTHER_SENDER_1,
            edit("filler-edit-3", TARGET, "edited by someone else")),
        raw(
            fillerMam(FILLERS + 5),
            OTHER_SENDER_1,
            reaction("filler-react-1", TARGET, "\uD83D\uDC4D")),
        raw(
            fillerMam(FILLERS + 6),
            OTHER_SENDER_2,
            reaction("filler-react-2", TARGET, "\uD83D\uDD25")),
        raw(fillerMam(FILLERS + 7), OTHER_SENDER_2, reaction("filler-react-3", TARGET, "\u2764")),
        raw(
            fillerMam(FILLERS + 8),
            OTHER_SENDER_3,
            reaction("filler-react-4", TARGET, "\uD83C\uDF89")),
        raw(
            fillerMam(FILLERS + 10),
            OTHER_SENDER_2,
            edit("filler-edit-4", DELETED_TARGET, "edit of a deleted message")),
        raw(
            replyToTargetMam,
            FILLER_SENDER,
            reply("filler-reply-2", TARGET, "reply to the target")),
        "INSERT INTO smart_markers (lserver, luser, to_jid, thread, type, msg_id, timestamp)"
            + " SELECT lserver, '"
            + MARKER_USER
            + "', to_jid, thread, type, '"
            + MARKED_CLIENT_ID
            + "', "
            + (MARKED_AT_MARKER >> 8)
            + " FROM smart_markers WHERE to_jid = '"
            + ROOM
            + "@muclight.carbonio' AND type = 'D'");

    ImportRun run =
        MongooseImport.run(
            database.newTargetDatabase(),
            database.credentialsFor(sourceDatabase),
            MongooseImport.DUMP_ZONE);
    assertTrue(run.succeeded(), run::failureText);
    padded = run.chats();
    source = readSource(database, sourceDatabase);
    pinStanza = readPinStanza(database, sourceDatabase);

    excludedIds = new HashSet<>();
    for (int k = 1; k <= FILLERS; k++) {
      excludedIds.add(ExpectedIds.message(ROOM, fillerMam(k)));
    }
    excludedIds.add(ExpectedIds.message(ROOM, edgeReplyMam));
    excludedIds.add(ExpectedIds.message(ROOM, TARGET));
    excludedIds.add(ExpectedIds.message(ROOM, replyToTargetMam));
  }

  private static List<SourceRow> readSource(MongooseImportDatabase database, String name)
      throws SQLException {
    List<SourceRow> rows = new ArrayList<>();
    try (Connection connection = database.connect(name);
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT m.id, s.user_name, m.message FROM mam_muc_message m"
                    + " JOIN mam_server_user r ON r.id = m.room_id"
                    + " AND r.server = 'muclight.carbonio' AND r.user_name = ?"
                    + " LEFT JOIN mam_server_user s ON s.id = m.sender_id AND s.server = 'carbonio'"
                    + " ORDER BY m.id")) {
      statement.setString(1, ROOM);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          rows.add(
              new SourceRow(rs.getLong(1), rs.getString(2), StanzaParser.parse(rs.getBytes(3))));
        }
      }
    }
    return rows;
  }

  private static String stanza(String clientId, String payload) {
    return "<message from='"
        + ROOM
        + "@muclight.carbonio/"
        + FILLER_SENDER
        + "@carbonio' id='"
        + clientId
        + "' type='groupchat'>"
        + payload
        + "</message>";
  }

  private static String text(String clientId, String body) {
    return stanza(clientId, "<body>" + body + "</body>");
  }

  private static String reply(String clientId, long target, String body) {
    return stanza(
        clientId,
        "<body>"
            + body
            + "</body><reply id='"
            + StanzaIds.encode(target)
            + "' xmlns='urn:xmpp:reply:0'/>");
  }

  private static String edit(String clientId, long target, String body) {
    String id = StanzaIds.encode(target);
    return stanza(
        clientId,
        "<apply-to id='"
            + id
            + "' parent-id='"
            + id
            + "' xmlns='urn:xmpp:fasten:0'><edit xmlns='zextras:xmpp:edit:0'/><external"
            + " name='body'/></apply-to><body>"
            + body
            + "</body>");
  }

  private static String reaction(String clientId, long target, String body) {
    return stanza(
        clientId,
        "<apply-to id='"
            + StanzaIds.encode(target)
            + "' xmlns='urn:xmpp:fasten:0'><reaction xmlns='zextras:xmpp:reaction:0'/><external"
            + " name='body'/></apply-to><body>"
            + body
            + "</body>");
  }

  private static String raw(long mam, String sender, String xml) {
    return "INSERT INTO mam_muc_message (id, room_id, sender_id, nick_name, message, search_body,"
        + " origin_id) SELECT "
        + mam
        + ", t.room_id, (SELECT id FROM mam_server_user WHERE server = 'carbonio' AND user_name ="
        + " '"
        + sender
        + "'), t.nick_name, convert_to($x$"
        + xml
        + "$x$, 'UTF8'), NULL, NULL FROM mam_muc_message t WHERE t.id = "
        + TEMPLATE_TEXT
        + " AND t.room_id = (SELECT id FROM mam_server_user WHERE server = 'muclight.carbonio'"
        + " AND user_name = '"
        + ROOM
        + "')";
  }

  private static String readPinStanza(MongooseImportDatabase database, String name)
      throws SQLException {
    try (Connection connection = database.connect(name);
        PreparedStatement statement =
            connection.prepareStatement("SELECT stanza_id FROM pin_message WHERE room_id = ?")) {
      statement.setString(1, ROOM + "@muclight.carbonio");
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    }
  }

  private static boolean afterFillers(SourceRow row) {
    return row.mam() > fillerMam(FILLERS);
  }

  private static OptionalLong olderTarget(String stanzaId) {
    OptionalLong mam = StanzaIds.decode(stanzaId);
    return mam.isPresent() && mam.getAsLong() <= LAST_ROW_BEFORE_FILLERS
        ? mam
        : OptionalLong.empty();
  }

  private static Optional<Message> messageAt(long mam) {
    return padded.message(ROOM, mam);
  }

  private static List<String> tables() {
    return plain.query(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'chats'"
            + " AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'"
            + " ORDER BY table_name",
        rs -> rs.getString(1));
  }

  private static List<String> rows(ImportedChats chats, String table) {
    return chats.query(
        "SELECT t::text FROM chats." + table + " t ORDER BY 1", rs -> rs.getString(1));
  }

  private static boolean belongsToTheAddedRows(String table, String row) {
    if ("read_marker".equals(table) && row.startsWith("(" + MARKER_USER + "," + ROOM + ",")) {
      return true;
    }
    Matcher matcher = UUID_PATTERN.matcher(row);
    while (matcher.find()) {
      if (excludedIds.contains(matcher.group())) {
        return true;
      }
    }
    return false;
  }

  @Test
  @DisplayName("WINDOW-1: the padding exceeds the window and every filler is imported")
  void paddingExceedsTheWindow() {
    assertTrue(FILLERS > WINDOW);
    assertEquals(
        plain.count("SELECT count(*) FROM chats.message WHERE room_id = ?", ROOM) + FILLERS + 3,
        padded.count("SELECT count(*) FROM chats.message WHERE room_id = ?", ROOM));
    assertTrue(messageAt(fillerMam(1)).isPresent());
    assertTrue(messageAt(fillerMam(FILLERS)).isPresent());
    assertEquals(FILLER_SENDER, messageAt(fillerMam(FILLERS)).orElseThrow().senderId());
  }

  @Test
  @DisplayName(
      "WINDOW-2: every table equals the unpadded import, less the added rows and the added marker")
  void everyTableMatchesTheUnpaddedImport() {
    List<String> tables = tables();
    long compared = 0;
    for (String table : tables) {
      List<String> expected = rows(plain, table);
      List<String> actual = new ArrayList<>();
      for (String row : rows(padded, table)) {
        if (!belongsToTheAddedRows(table, row)) {
          actual.add(row);
        }
      }
      compared += expected.size();
      assertEquals(expected, actual, "table " + table);
    }
    assertTrue(tables.contains("message") && tables.contains("read_marker"));
    assertTrue(compared > 1000, "rows compared: " + compared);
  }

  @Test
  @DisplayName("WINDOW-3: replies after the window point to the message they answered")
  void repliesToOlderMessages() {
    int crossing = 0;
    for (SourceRow row : source) {
      ParsedRow p = row.parsed();
      if (!afterFillers(row)
          || (p.kind() != Kind.TEXT && p.kind() != Kind.ATTACHMENT)
          || p.replyRef() == null) {
        continue;
      }
      OptionalLong target = olderTarget(p.replyRef());
      if (target.isEmpty() || messageAt(target.getAsLong()).isEmpty()) {
        continue;
      }
      Optional<Message> reply = messageAt(row.mam());
      if (reply.isPresent()) {
        crossing++;
        assertEquals(
            ExpectedIds.message(ROOM, target.getAsLong()),
            reply.get().replyToId(),
            "reply " + row.mam());
      }
    }
    assertTrue(crossing > 0, "replies to a message before the fillers: " + crossing);
  }

  @Test
  @DisplayName("WINDOW-4: a reply to the stanza of an old edit resolves to the edited message")
  void replyToAnOldEditRow() {
    SourceRow edit =
        source.stream().filter(r -> r.mam() == LAST_ROW_BEFORE_FILLERS).findFirst().orElseThrow();
    assertEquals(Kind.EDIT, edit.parsed().kind());
    SourceRow reply =
        source.stream().filter(r -> r.mam() == edgeReplyMam).findFirst().orElseThrow();
    assertEquals(StanzaIds.encode(LAST_ROW_BEFORE_FILLERS), reply.parsed().replyRef());

    long original = StanzaIds.decode(edit.parsed().applyToRef()).orElseThrow();
    assertTrue(messageAt(original).isPresent());
    assertEquals(
        ExpectedIds.message(ROOM, original), messageAt(edgeReplyMam).orElseThrow().replyToId());
  }

  @Test
  @DisplayName("WINDOW-5: edits after the window fold into older messages, the last edit wins")
  void editsOfOlderMessages() {
    Map<Long, SourceRow> lastEdit = new LinkedHashMap<>();
    for (SourceRow row : source) {
      ParsedRow p = row.parsed();
      if (!afterFillers(row) || p.kind() != Kind.EDIT || row.sender() == null) {
        continue;
      }
      OptionalLong target = olderTarget(p.applyToRef());
      if (target.isEmpty()) {
        continue;
      }
      Optional<Message> message = messageAt(target.getAsLong());
      if (message.isPresent()
          && !message.get().deleted()
          && row.sender().equals(message.get().senderId())) {
        lastEdit.put(target.getAsLong(), row);
      }
    }
    for (Map.Entry<Long, SourceRow> edit : lastEdit.entrySet()) {
      Message message = messageAt(edit.getKey()).orElseThrow();
      assertTrue(message.edited(), "edited " + edit.getKey());
      assertEquals(edit.getValue().parsed().text(), message.text(), "text " + edit.getKey());
    }
    assertEquals("edited twice", messageAt(TARGET).orElseThrow().text());
    assertTrue(lastEdit.size() > 0, "older messages edited after the fillers: " + lastEdit.size());
  }

  @Test
  @DisplayName("WINDOW-6: reactions after the window attach to older live messages")
  void reactionsToOlderMessages() {
    Map<String, SourceRow> latest = new LinkedHashMap<>();
    for (SourceRow row : source) {
      ParsedRow p = row.parsed();
      if (!afterFillers(row)
          || p.kind() != Kind.REACTION
          || row.sender() == null
          || p.text().length() > 32) {
        continue;
      }
      OptionalLong target = olderTarget(p.applyToRef());
      if (target.isPresent()) {
        latest.put(target.getAsLong() + "/" + row.sender(), row);
      }
    }
    int attached = 0;
    for (SourceRow row : latest.values()) {
      long target = StanzaIds.decode(row.parsed().applyToRef()).orElseThrow();
      Optional<Message> message = messageAt(target);
      if (message.isEmpty() || message.get().deleted() || row.parsed().text().isEmpty()) {
        continue;
      }
      attached++;
      assertTrue(
          padded.reactions(message.get().id()).stream()
              .anyMatch(
                  r -> r.userId().equals(row.sender()) && r.reaction().equals(row.parsed().text())),
          "reaction " + row.mam());
    }
    assertTrue(attached > 0, "reactions on older live messages: " + attached);
  }

  @Test
  @DisplayName(
      "WINDOW-7: edits and reactions after the window do not touch an older deleted message")
  void fasteningsOfOlderDeletedMessages() {
    int edits = 0;
    int reactions = 0;
    for (SourceRow row : source) {
      ParsedRow p = row.parsed();
      if (!afterFillers(row) || (p.kind() != Kind.EDIT && p.kind() != Kind.REACTION)) {
        continue;
      }
      OptionalLong target = olderTarget(p.applyToRef());
      Optional<Message> message =
          target.isPresent() ? messageAt(target.getAsLong()) : Optional.empty();
      if (message.isEmpty() || !message.get().deleted()) {
        continue;
      }
      assertFalse(message.get().edited(), "deleted message edited " + target.getAsLong());
      assertTrue(padded.reactions(message.get().id()).isEmpty());
      if (p.kind() == Kind.EDIT) {
        edits++;
      } else {
        reactions++;
      }
    }
    assertTrue(edits > 0, "edits aimed at an older deleted message: " + edits);
    assertTrue(reactions > 0, "reactions aimed at an older deleted message: " + reactions);
  }

  @Test
  @DisplayName("WINDOW-8: pin events after the window name the older pinned message")
  void pinEventsOfOlderMessages() {
    int crossing = 0;
    for (SourceRow row : source) {
      ParsedRow p = row.parsed();
      if (!afterFillers(row) || p.kind() != Kind.PINNED || row.sender() == null) {
        continue;
      }
      OptionalLong target = olderTarget(p.pinRef());
      if (target.isEmpty() || messageAt(target.getAsLong()).isEmpty()) {
        continue;
      }
      ImportedChats.SystemEvent event = padded.systemEvent(ROOM, row.mam()).orElseThrow();
      assertEquals("MESSAGE_PINNED", event.type());
      assertEquals(
          ExpectedIds.message(ROOM, target.getAsLong()), event.json().get("messageId").asText());
      crossing++;
    }
    assertTrue(crossing > 0, "pin events on an older message: " + crossing);
  }

  @Test
  @DisplayName("WINDOW-9: the room pin row resolves its message after the window")
  void roomPinOfAnOlderMessage() {
    long target = olderTarget(pinStanza).orElseThrow();
    Message message = messageAt(target).orElseThrow();
    assertFalse(message.deleted());
    assertEquals(ExpectedIds.message(ROOM, target), padded.pin(ROOM).orElseThrow().messageId());
  }

  @Test
  @DisplayName(
      "WINDOW-10: a read marker names the latest row up to its time, found while streaming")
  void readMarkerOfAnOlderRow() {
    Message atMarker = messageAt(MARKED_AT_MARKER).orElseThrow();
    Message first = messageAt(MARKED_FIRST).orElseThrow();
    Message later = messageAt(MARKED_LATER).orElseThrow();
    assertNotEquals(atMarker.createdAt(), first.createdAt());
    assertNotEquals(atMarker.createdAt(), later.createdAt());
    assertEquals(
        atMarker.createdAt(), padded.readMarker(MARKER_USER, ROOM).orElseThrow().itemCreatedAt());
    assertTrue(MARKED_AT_MARKER < LAST_ROW_BEFORE_FILLERS);
  }
}
