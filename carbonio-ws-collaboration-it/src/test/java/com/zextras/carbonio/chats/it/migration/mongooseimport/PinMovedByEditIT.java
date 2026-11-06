// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.migration.mongooseimport.StanzaIds;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.Pin;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import: a pin that MongooseIM moved to an edit keeps the archived pin action")
class PinMovedByEditIT {

  private static final String ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String AUTHOR = "97c353af-4374-5ff3-a3eb-3056c0b4dba9";
  private static final String PINNER = "7d03b0c9-cd6e-5392-9186-1b9646f07fef";
  private static final String RECIPIENT = "7d7ebbe1-c1c7-513f-bae9-24131c672d1d";
  private static final long TEMPLATE_TEXT = 458596911276023041L;
  private static final DateTimeFormatter NAIVE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

  private static long lastMam;
  private static ImportRun movedUtc;
  private static ImportRun movedRome;
  private static ImportRun repinnedAfterUnpin;
  private static ImportRun otherMessageLastPinned;

  private static long mam(int k) {
    return (((lastMam >> 8) + k * 1_000_000L) << 8) | (lastMam & 255);
  }

  private static String naive(LocalDateTime time) {
    return "TIMESTAMP '" + NAIVE.format(time) + "'";
  }

  @BeforeAll
  static void importScenarios() throws SQLException {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    String probe = database.newSourceDatabase();
    try (Connection connection = database.connect(probe);
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

    LocalDateTime editTime = ExpectedTimes.mamTime(mam(3));
    String moved =
        importWith(
            database,
            List.of(
                raw(mam(1), AUTHOR, text("pin-target", "pinned then edited")),
                raw(mam(2), PINNER, pinLine("pin-line", "messagePinned", PINNER, mam(1))),
                raw(mam(3), AUTHOR, edit("pin-edit", mam(1), "edited after the pin")),
                pinRow(mam(3), RECIPIENT, naive(editTime.plus(Duration.ofHours(2))))));
    movedUtc = run(database, moved, ZoneId.of("UTC"));
    movedRome = run(database, moved, ZoneId.of("Europe/Rome"));

    String repinned =
        importWith(
            database,
            List.of(
                raw(mam(1), AUTHOR, text("pin-target", "pinned, unpinned, pinned again")),
                raw(mam(2), PINNER, pinLine("pin-line", "messagePinned", PINNER, mam(1))),
                raw(mam(3), PINNER, pinLine("unpin-line", "messageUnpinned", PINNER, mam(1))),
                pinRow(mam(1), RECIPIENT, naive(ExpectedTimes.mamTime(mam(3)).plusSeconds(1)))));
    repinnedAfterUnpin = run(database, repinned, ZoneId.of("UTC"));

    String other =
        importWith(
            database,
            List.of(
                raw(mam(1), AUTHOR, text("pin-target", "pinned and edited, then not pinned")),
                raw(mam(2), AUTHOR, text("other-target", "the message pinned last")),
                raw(mam(3), PINNER, pinLine("pin-line", "messagePinned", PINNER, mam(2))),
                raw(mam(4), AUTHOR, edit("pin-edit", mam(1), "edited")),
                pinRow(mam(4), RECIPIENT, naive(ExpectedTimes.mamTime(mam(4))))));
    otherMessageLastPinned = run(database, other, ZoneId.of("UTC"));
  }

  private static String importWith(MongooseImportDatabase database, List<String> statements) {
    String source = database.newSourceDatabase();
    database.execute(source, statements.toArray(String[]::new));
    return source;
  }

  private static ImportRun run(MongooseImportDatabase database, String source, ZoneId zone) {
    ImportRun run =
        MongooseImport.run(
            database.newTargetDatabase(),
            database.credentialsFor(source),
            zone,
            MongooseImport.DUMP_ZONE);
    assertTrue(run.succeeded(), run::failureText);
    return run;
  }

  private static String stanza(String sender, String clientId, String payload) {
    return "<message from='"
        + ROOM
        + "@muclight.carbonio/"
        + sender
        + "@carbonio' id='"
        + clientId
        + "' type='groupchat'>"
        + payload
        + "</message>";
  }

  private static String text(String clientId, String body) {
    return stanza(AUTHOR, clientId, "<body>" + body + "</body>");
  }

  private static String edit(String clientId, long target, String body) {
    String id = StanzaIds.encode(target);
    return stanza(
        AUTHOR,
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

  private static String pinLine(String clientId, String operation, String pinner, long target) {
    return stanza(
        pinner,
        clientId,
        "<x xmlns='urn:xmpp:muclight:0#configuration'><operation>"
            + operation
            + "</operation><pin message-id='"
            + StanzaIds.encode(target)
            + "' xmlns='zextras:iq:pin'><pinned-by>"
            + pinner
            + "@carbonio/res</pinned-by><room-id>"
            + ROOM
            + "@muclight.carbonio</room-id><body></body></pin></x><body/>");
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
        + TEMPLATE_TEXT;
  }

  private static String pinRow(long stanzaMam, String pinnedBy, String pinnedAt) {
    return "DELETE FROM pin_message WHERE room_id = '"
        + ROOM
        + "@muclight.carbonio';"
        + " INSERT INTO pin_message (request_id, pinned_by, room_id, luser, server, stanza_id,"
        + " pinned_at) VALUES ('req', '"
        + pinnedBy
        + "@carbonio', '"
        + ROOM
        + "@muclight.carbonio', '"
        + pinnedBy
        + "', 'carbonio', '"
        + StanzaIds.encode(stanzaMam)
        + "', "
        + pinnedAt
        + ")";
  }

  private static int unarchivedPins(ImportRun run) {
    return run.logLines().stream()
        .filter(l -> l.startsWith("WARN") && l.contains("pins without an archived pin line: "))
        .map(l -> l.replaceAll(".*pins without an archived pin line: (\\d+),.*", "$1"))
        .mapToInt(Integer::parseInt)
        .findFirst()
        .orElse(0);
  }

  @Test
  @DisplayName("The pin keeps the original message, the archived pinner and the archived time")
  void movedPinUsesTheArchivedAction() {
    Pin pin = movedUtc.chats().pin(ROOM).orElseThrow();

    assertEquals(ExpectedIds.message(ROOM, mam(1)), pin.messageId());
    assertEquals(PINNER, pin.pinnedBy());
    assertEquals(ExpectedTimes.mamTime(mam(2)), pin.pinnedAt());
  }

  @Test
  @DisplayName("The moved pin does not depend on the JVM zone")
  void movedPinIsZoneIndependent() {
    assertEquals(movedUtc.chats().pin(ROOM), movedRome.chats().pin(ROOM));
  }

  @Test
  @DisplayName("A pin repeated after an unpin without an archived line keeps MongooseIM's values")
  void repinWithoutArchivedLineKeepsThePinRow() {
    Pin pin = repinnedAfterUnpin.chats().pin(ROOM).orElseThrow();

    assertEquals(ExpectedIds.message(ROOM, mam(1)), pin.messageId());
    assertEquals(RECIPIENT, pin.pinnedBy());
    assertEquals(ExpectedTimes.mamTime(mam(3)).plusSeconds(1), pin.pinnedAt());
    assertEquals(unarchivedPins(movedUtc) + 1, unarchivedPins(repinnedAfterUnpin));
  }

  @Test
  @DisplayName("An archived pin of another message is not taken for an edited pinned message")
  void lastLinePinningAnotherMessageIsIgnored() {
    Pin pin = otherMessageLastPinned.chats().pin(ROOM).orElseThrow();

    assertEquals(ExpectedIds.message(ROOM, mam(1)), pin.messageId());
    assertEquals(RECIPIENT, pin.pinnedBy());
    assertEquals(ExpectedTimes.mamTime(mam(4)), pin.pinnedAt());
  }
}
