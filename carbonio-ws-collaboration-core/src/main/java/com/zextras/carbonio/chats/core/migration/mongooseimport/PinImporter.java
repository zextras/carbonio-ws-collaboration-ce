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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

public final class PinImporter implements AutoCloseable {

  public record Pin(String pinnedBy, String stanzaId, Instant pinnedAt) {}

  public record PinLine(
      boolean pinned, long stanzaMam, String messageId, String pinner, Instant time) {}

  private static final String SELECT =
      "SELECT pinned_by, stanza_id, pinned_at FROM pin_message WHERE room_id = ?";

  private static final String INSERT =
      "INSERT INTO chats.message_pin (room_id, message_id, pinned_by, pinned_at)"
          + " VALUES (?, ?, ?, ?)";

  private final PreparedStatement select;
  private final PreparedStatement insert;
  private static final LocalDateTime EARLIEST = LocalDateTime.of(2000, 1, 1, 0, 0);

  private final ZoneId zone;
  private final ImportContext context;
  private int unarchived;

  public PinImporter(
      MongooseSource.Ready source, Connection target, ZoneId zone, ImportContext context)
      throws SQLException {
    this.zone = zone;
    this.context = context;
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
        pins.add(new Pin(rs.getString(1), rs.getString(2), pinTime(readLocal(rs), room.id())));
      }
    }
    return pins;
  }

  private static LocalDateTime readLocal(ResultSet rs) {
    try {
      return rs.getObject(3, LocalDateTime.class);
    } catch (SQLException | RuntimeException e) {
      return LocalDateTime.MAX;
    }
  }

  private Instant pinTime(LocalDateTime local, String roomId) {
    if (local == null) {
      return null;
    }
    if (local.isBefore(EARLIEST) || local.getYear() > 9999) {
      context.count(SkipReason.PIN_TIME_OUT_OF_RANGE, roomId, 0);
      return null;
    }
    Instant instant = local.atZone(zone).toInstant();
    if (instant.isAfter(context.t0())) {
      context.count(SkipReason.PIN_TIME_OUT_OF_RANGE, roomId, 0);
      return null;
    }
    return instant;
  }

  public void importRoomPin(
      TargetRoom room,
      RoomRows rows,
      List<Pin> pins,
      Map<String, Instant> pinTimes,
      PinLine lastLine)
      throws SQLException {
    for (Pin pin : pins) {
      String pinner = pinner(pin.pinnedBy());
      String stanza = pin.stanzaId();
      if (pinner == null) {
        context.count(SkipReason.PIN_BAD_USER, room.id(), 0);
        continue;
      }
      if (stanza == null || stanza.isBlank()) {
        context.count(SkipReason.PIN_UNRESOLVED, room.id(), 0);
        continue;
      }
      OptionalLong mam = StanzaIds.decode(stanza.strip());
      if (mam.isEmpty()) {
        context.count(SkipReason.PIN_UNRESOLVED, room.id(), 0);
        continue;
      }
      RoomRows.Row row = rows.resolveStanza(mam.getAsLong());
      if (row == null || row.outcome() != RoomRows.MESSAGE || row.deleted()) {
        context.count(SkipReason.PIN_UNRESOLVED, room.id(), mam.getAsLong());
        continue;
      }
      Instant pinnedAt;
      if (movedByEdit(lastLine, mam.getAsLong(), row)) {
        pinner = lastLine.pinner();
        pinnedAt = lastLine.time();
      } else {
        Instant archived = pinTimes.get(pinKey(mam.getAsLong(), pinner));
        pinnedAt = sameAction(archived, pin.pinnedAt());
        if (pinnedAt != null && !pinnedAt.equals(archived)) {
          unarchived++;
        }
      }
      if (pinnedAt == null) {
        continue;
      }
      insert.setString(1, room.id());
      insert.setString(2, row.itemId());
      insert.setString(3, pinner);
      insert.setObject(4, pinnedAt.atOffset(ZoneOffset.UTC));
      insert.executeUpdate();
    }
  }

  public int unarchived() {
    return unarchived;
  }

  // Editing a pinned message makes MongooseIM point the pin at the edit, with now() and the
  // recipient as pinner, and archive nothing: the room's last pin line keeps the real action.
  private static boolean movedByEdit(PinLine lastLine, long pinStanzaMam, RoomRows.Row row) {
    return lastLine != null
        && lastLine.pinned()
        && lastLine.stanzaMam() != pinStanzaMam
        && row.itemId().equals(lastLine.messageId());
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
  private static Instant sameAction(Instant archived, Instant pinnedAt) {
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
