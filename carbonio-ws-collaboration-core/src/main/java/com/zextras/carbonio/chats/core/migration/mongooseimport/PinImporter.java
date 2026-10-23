// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

public final class PinImporter implements AutoCloseable {

  public record Pin(String pinnedBy, String stanzaId, LocalDateTime pinnedAt) {}

  private static final String SELECT =
      "SELECT pinned_by, stanza_id, pinned_at FROM pin_message WHERE room_id = ?";

  private static final String INSERT =
      "INSERT INTO chats.message_pin (room_id, message_id, pinned_by, pinned_at)"
          + " VALUES (?, ?, ?, ?)";

  private final PreparedStatement select;
  private final PreparedStatement insert;

  public PinImporter(MongooseSource.Ready source, Connection target) throws SQLException {
    this.select = source.hasPins() ? source.connection().prepareStatement(SELECT) : null;
    this.insert = target.prepareStatement(INSERT);
  }

  public List<Pin> read(TargetRoom room) throws SQLException {
    List<Pin> pins = new ArrayList<>();
    if (select == null) {
      return pins;
    }
    select.setString(1, room.id() + "@muclight.carbonio");
    try (ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        pins.add(new Pin(rs.getString(1), rs.getString(2), rs.getObject(3, LocalDateTime.class)));
      }
    }
    return pins;
  }

  public void importRoomPin(
      TargetRoom room, RoomRows rows, List<Pin> pins, Map<String, LocalDateTime> pinTimes)
      throws SQLException {
    for (Pin pin : pins) {
      String pinner = pinner(pin.pinnedBy());
      String stanza = pin.stanzaId();
      if (pinner == null || stanza == null || stanza.isBlank()) {
        continue;
      }
      OptionalLong mam = StanzaIds.decode(stanza.strip());
      if (mam.isEmpty()) {
        continue;
      }
      RoomRows.Row row = rows.resolveStanza(mam.getAsLong());
      if (row == null || row.outcome() != RoomRows.MESSAGE || row.deleted()) {
        continue;
      }
      LocalDateTime pinnedAt =
          sameAction(pinTimes.get(pinKey(mam.getAsLong(), pinner)), pin.pinnedAt());
      if (pinnedAt == null) {
        continue;
      }
      insert.setString(1, room.id());
      insert.setString(2, row.itemId());
      insert.setString(3, pinner);
      insert.setObject(4, pinnedAt);
      insert.executeUpdate();
    }
  }

  @Override
  public void close() throws SQLException {
    if (select != null) {
      select.close();
    }
    insert.close();
  }

  // pinned_at is PostgreSQL now() in the MongooseIM session zone: the archived pin row is the same
  // action when the two differ by whole quarter hours (a zone offset) plus less than a second.
  private static LocalDateTime sameAction(LocalDateTime archived, LocalDateTime pinnedAt) {
    if (archived == null) {
      return pinnedAt;
    }
    if (pinnedAt == null) {
      return archived;
    }
    long millis = Duration.between(archived, pinnedAt).toMillis();
    long quarter = Duration.ofMinutes(15).toMillis();
    long residual = Math.floorMod(millis + quarter / 2, quarter) - quarter / 2;
    boolean zoneOffset = Math.abs(millis) <= Duration.ofHours(14).toMillis() + 1000;
    return zoneOffset && Math.abs(residual) <= 1000 ? archived : pinnedAt;
  }

  static String pinKey(long stanzaMam, String pinner) {
    return stanzaMam + "/" + pinner;
  }

  private static String pinner(String raw) {
    if (raw == null) {
      return null;
    }
    int at = raw.indexOf('@');
    String candidate = (at < 0 ? raw : raw.substring(0, at)).strip().toLowerCase(Locale.ROOT);
    return CanonicalUuid.matches(candidate) ? candidate : null;
  }
}
