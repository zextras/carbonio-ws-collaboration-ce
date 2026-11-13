// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.migration.mongooseimport.StanzaIds;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.Message;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.SystemEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import: broken source data never fails the import, it is clamped or skipped")
class ImportRobustnessIT {

  private static final String ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String AUTHOR = "97c353af-4374-5ff3-a3eb-3056c0b4dba9";
  private static final String PINNER = "7d03b0c9-cd6e-5392-9186-1b9646f07fef";
  private static final String LAST_SEEN_ZERO = "00000000-0000-4000-8000-0000000000a1";
  private static final String LAST_SEEN_FUTURE = "00000000-0000-4000-8000-0000000000a2";
  private static final long TEMPLATE_TEXT = 458596911276023041L;
  private static final long FUTURE_MICROS = 4_070_908_800_000_000L;

  private static long lastMam;
  private static ImportRun run;
  private static ImportedChats chats;

  private static long mam(int k) {
    return (((lastMam >> 8) + k * 1_000_000L) << 8) | (lastMam & 255);
  }

  private static long futureMam() {
    return (FUTURE_MICROS << 8) | (lastMam & 255);
  }

  @BeforeAll
  static void importBrokenSource() throws SQLException {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    String source = database.newSourceDatabase();
    try (Connection connection = database.connect(source);
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT max(m.id) FROM mam_muc_message m JOIN mam_server_user r ON r.id = m.room_id"
                    + " WHERE r.server = 'muclight.carbonio' AND r.user_name = '"
                    + ROOM
                    + "'")) {
      rs.next();
      lastMam = rs.getLong(1);
    }
    database.execute(
        source,
        text(mam(1), "kept"),
        raw(mam(2), AUTHOR, utf8("<message><body>truncated")),
        raw(mam(3), AUTHOR, "decode('836802', 'hex')"),
        raw(mam(4), AUTHOR, utf8("<iq><body>x</body></iq>")),
        raw(
            mam(5),
            AUTHOR,
            utf8(
                stanza(
                    AUTHOR,
                    "<body>fix</body><replace id='a' xmlns='urn:xmpp:message-correct:0'/>"))),
        raw(
            mam(6),
            AUTHOR,
            utf8(
                stanza(
                    AUTHOR,
                    "<x xmlns='urn:xmpp:muclight:0#configuration'><operation>somethingNew"
                        + "</operation></x><body/>"))),
        raw(mam(7), "ghost", utf8(stanza("ghost", "<body>nobody</body>"))),
        raw(
            mam(8),
            AUTHOR,
            utf8(
                stanza(
                    AUTHOR,
                    "<body/><forwarded xmlns='urn:xmpp:forward:0'><delay"
                        + " stamp='2026-10-07T16:38:56Z' xmlns='urn:xmpp:delay'/><message"
                        + " from='"
                        + ROOM
                        + "@muclight.carbonio'><body>bare forward</body></message></forwarded>"))),
        raw(
            mam(9),
            AUTHOR,
            utf8(
                stanza(
                    AUTHOR,
                    "<body/><forwarded xmlns='urn:xmpp:forward:0'><message from='"
                        + ROOM
                        + "@muclight.carbonio/"
                        + PINNER
                        + "@carbonio'><body>no stamp</body></message></forwarded>"))),
        text(futureMam(), "from the future"),
        "DELETE FROM pin_message WHERE room_id = '" + ROOM + "@muclight.carbonio'",
        "INSERT INTO pin_message (request_id, pinned_by, room_id, luser, server, stanza_id,"
            + " pinned_at) VALUES ('req', '"
            + PINNER
            + "@carbonio', '"
            + ROOM
            + "@muclight.carbonio', '"
            + PINNER
            + "', 'carbonio', '"
            + StanzaIds.encode(mam(1))
            + "', 'infinity')",
        "INSERT INTO last (server, username, seconds, state) VALUES ('carbonio', '"
            + LAST_SEEN_ZERO
            + "', 0, ''), ('carbonio', '"
            + LAST_SEEN_FUTURE
            + "', 2000000000, ''), ('carbonio', 'not-a-uuid', 1700000000, '')");

    String target = database.newTargetDatabase();
    database.execute(
        target, "UPDATE chats.room SET created_at = 'infinity' WHERE id = '" + ROOM + "'");
    run = MongooseImport.run(target, database.credentialsFor(source));
    chats = run.chats();
  }

  private static String stanza(String sender, String payload) {
    return "<message from='"
        + ROOM
        + "@muclight.carbonio/"
        + sender
        + "@carbonio' id='"
        + Long.toHexString(System.nanoTime())
        + "' type='groupchat'>"
        + payload
        + "</message>";
  }

  private static String utf8(String xml) {
    return "convert_to($x$" + xml + "$x$, 'UTF8')";
  }

  private static String text(long mam, String body) {
    return raw(mam, AUTHOR, utf8(stanza(AUTHOR, "<body>" + body + "</body>")));
  }

  private static String raw(long mam, String sender, String messageExpression) {
    return "INSERT INTO mam_muc_message (id, room_id, sender_id, nick_name, message, search_body,"
        + " origin_id) SELECT "
        + mam
        + ", t.room_id, (SELECT id FROM mam_server_user WHERE server = 'carbonio' AND user_name ="
        + " '"
        + sender
        + "'), t.nick_name, "
        + messageExpression
        + ", NULL, NULL FROM mam_muc_message t WHERE t.id = "
        + TEMPLATE_TEXT;
  }

  private static int count(String reason) {
    Pattern pattern = Pattern.compile("\\b" + reason + "=(\\d+)");
    return run.logLines().stream()
        .filter(l -> l.startsWith("WARN") && l.contains("skipped or adjusted source data"))
        .map(pattern::matcher)
        .filter(Matcher::find)
        .mapToInt(m -> Integer.parseInt(m.group(1)))
        .findFirst()
        .orElse(0);
  }

  @Test
  @DisplayName("The import completes and reports a single WARN summary line")
  void importCompletesWithSummary() {
    assertTrue(run.succeeded(), run::failureText);
    assertEquals(
        1,
        run.logLines().stream()
            .filter(l -> l.startsWith("WARN") && l.contains("skipped or adjusted source data"))
            .count());
    assertTrue(
        run.logLines().stream().anyMatch(l -> l.contains("MongooseIM import completed: rooms=")));
  }

  @Test
  @DisplayName("Every skip reason that was injected is counted")
  void skipReasonsAreCounted() {
    assertAll(
        () -> assertTrue(count("row_invalid_xml") >= 1),
        () -> assertTrue(count("row_binary") >= 1),
        () -> assertTrue(count("row_not_message") >= 1),
        () -> assertTrue(count("message_correct") >= 1),
        () -> assertTrue(count("unknown_config_op") >= 1),
        () -> assertTrue(count("invalid_sender") >= 1),
        () -> assertTrue(count("forward_stamp_missing") >= 1),
        () -> assertTrue(count("last_seen_nonpositive") >= 1),
        () -> assertTrue(count("last_seen_bad_user") >= 1));
  }

  @Test
  @DisplayName("A MAM time in the future is clamped to the import start")
  void futureMamTimeIsClamped() {
    Message message = chats.message(ROOM, futureMam()).orElseThrow();

    assertTrue(count("time_clamped") >= 1);
    assertFalse(message.createdAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1)));
    assertTrue(message.createdAt().isAfter(ExpectedTimes.mamTime(mam(9))));
  }

  @Test
  @DisplayName("A forward from a bare room jid is a plain message with the text kept")
  void bareRoomForwardIsPlain() {
    Message message = chats.message(ROOM, mam(8)).orElseThrow();

    assertAll(
        () -> assertFalse(message.forwarded()),
        () -> assertEquals("bare forward", message.text()),
        () -> assertNull(message.forwardedSenderId()),
        () -> assertNull(message.forwardedAt()),
        () -> assertTrue(count("forward_as_plain") >= 1));
  }

  @Test
  @DisplayName("A forward without a stamp has a NULL forwarded_at, not 1970")
  void forwardWithoutStampHasNullForwardedAt() {
    Message message = chats.message(ROOM, mam(9)).orElseThrow();

    assertAll(
        () -> assertTrue(message.forwarded()),
        () -> assertEquals(PINNER, message.forwardedSenderId()),
        () -> assertNull(message.forwardedAt()));
  }

  @Test
  @DisplayName("An infinite pinned_at is ignored: the archived path runs and no pin is written")
  void infinitePinnedAtIsIgnored() {
    assertAll(
        () -> assertTrue(chats.pin(ROOM).isEmpty()),
        () -> assertTrue(count("pin_time_out_of_range") >= 1));
  }

  @Test
  @DisplayName("An infinite room created_at is replaced before the creation event is written")
  void infiniteRoomCreatedAtIsReplaced() {
    List<SystemEvent> created =
        chats.systemEvents(ROOM).stream().filter(e -> e.type().equals("ROOM_CREATED")).toList();
    LocalDateTime firstMessage = chats.messages(ROOM).get(0).createdAt();

    assertEquals(1, created.size());
    assertTrue(created.get(0).createdAt().isBefore(firstMessage));
    assertTrue(count("room_created_at_out_of_range") >= 1);
  }

  @Test
  @DisplayName("Last seen: zero is skipped, a future value is clamped, a non-UUID is skipped")
  void lastSeenBounds() {
    assertAll(
        () -> assertTrue(chats.lastSeen(LAST_SEEN_ZERO).isEmpty()),
        () ->
            assertFalse(
                chats
                    .lastSeen(LAST_SEEN_FUTURE)
                    .orElseThrow()
                    .isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1))),
        () -> assertTrue(count("last_seen_future") >= 1));
  }
}
