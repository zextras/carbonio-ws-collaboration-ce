// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ParsedRow.Kind;
import com.zextras.carbonio.chats.core.migration.mongooseimport.RoomRows.Row;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public final class RoomImporter implements AutoCloseable, ImportPipeline.Target {

  public record Counts(int rooms, int messages, int events, int edits, int reactions) {}

  private record Context(TargetRoom room, boolean temporary, long floor, RoomClock clock) {
    boolean oneToOne() {
      return "ONE_TO_ONE".equals(room.type());
    }
  }

  private static final int ANALYZE_THRESHOLD = 1000;
  private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

  private final LegacyIds ids;
  private final ZoneId zone;
  private final FileLinker files;
  private final PinImporter pins;
  private final MarkerTranscriber markers;
  private final RoomRows rows;
  private final Fastenings fastenings;
  private final TimelineWriter writer;
  private final Map<String, LocalDateTime> pinTimes = new HashMap<>();

  private int rooms;
  private int edits;
  private int reactions;
  private Context context;
  private boolean archived;
  private long currentMam;

  public RoomImporter(
      Connection target,
      LegacyIds ids,
      ZoneId zone,
      FileLinker files,
      PinImporter pins,
      MarkerTranscriber markers)
      throws SQLException {
    this.ids = ids;
    this.zone = zone;
    this.files = files;
    this.pins = pins;
    this.markers = markers;
    this.rows = new RoomRows(target);
    this.fastenings = new Fastenings(target);
    this.writer = new TimelineWriter(target, files);
  }

  public Counts counts() {
    return new Counts(rooms, writer.messages(), writer.events(), edits, reactions);
  }

  @Override
  public String currentRoomId() {
    return context == null ? null : context.room().id();
  }

  @Override
  public long failedMam() {
    return writer.failedMam() != 0 ? writer.failedMam() : currentMam;
  }

  @Override
  public void close() throws SQLException {
    writer.close();
    fastenings.close();
    rows.close();
  }

  @Override
  public void beginRoom(TargetRoom room, long floor) throws SQLException {
    writer.beginRoom(room.id());
    rows.reset();
    fastenings.reset();
    pinTimes.clear();
    files.beginRoom(room.id());
    context = new Context(room, "TEMPORARY".equals(room.type()), floor, new RoomClock());
    archived = false;
    currentMam = 0;
  }

  @Override
  public void endRoom(
      List<PinImporter.Pin> pinRows, Map<String, MarkerTranscriber.Marker> markerRows)
      throws SQLException {
    writer.flush();
    rows.flush();
    currentMam = 0;
    if (!archived) {
      return;
    }
    TargetRoom room = context.room();
    if (rows.recorded() > ANALYZE_THRESHOLD || fastenings.staged() > ANALYZE_THRESHOLD) {
      rows.analyze();
      fastenings.analyze();
    }
    Fastenings.Counts folded = fastenings.fold();
    edits += folded.edits();
    reactions += folded.reactions();
    writer.roomCreated(room, ids.of(LegacyIds.ROOM_CREATED, room.id(), 0), context.clock());
    pins.importRoomPin(room, rows, pinRows, pinTimes);
    markers.endRoom(room, rows, markerRows);
    rooms++;
  }

  @Override
  public void handle(long mam, String server, String user, ParsedRow p) throws SQLException {
    Context c = context;
    currentMam = mam;
    archived = true;
    LocalDateTime raw = rawTime(mam);
    if (raw == null) {
      rows.record(p.rootId(), Row.notImported(mam, null, EPOCH));
      return;
    }
    String sender = "carbonio".equals(server) && validUser(user) ? user : null;
    if (mam < c.floor() || sender == null) {
      rows.record(p.rootId(), Row.notImported(mam, sender, raw));
      return;
    }
    String roomId = c.room().id();
    switch (p.kind()) {
      case TEXT -> {
        String id = ids.of(LegacyIds.MESSAGE, roomId, mam);
        LocalDateTime t = c.clock().next(raw);
        writer.addMessage(mam, TimelineWriter.Message.text(id, sender, p.text(), replyTo(p), t));
        rows.record(p.rootId(), Row.message(mam, sender, id, t, raw, false));
      }
      case TOMBSTONE -> {
        String id = ids.of(LegacyIds.MESSAGE, roomId, mam);
        LocalDateTime t = c.clock().next(raw);
        String attachment = files.onTombstone(mam, id);
        LocalDateTime deletedAt = local(p.retractedStamp(), t);
        writer.addMessage(
            mam, TimelineWriter.Message.tombstone(id, sender, attachment, deletedAt, t));
        rows.record(p.rootId(), Row.message(mam, sender, id, t, raw, true));
      }
      case FORWARD, ATTACHMENT -> attachmentOrForward(c, p, mam, sender, raw);
      case MEMBER_ADDED -> {
        if (c.oneToOne() || !validUser(p.configUserId())) {
          notImported(p, mam, sender, raw);
          return;
        }
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("addedByUserId", sender);
        content.putArray("addedUserIds").add(p.configUserId());
        event(c, p, mam, sender, raw, "MEMBER_ADDED", content);
      }
      case MEMBER_REMOVED -> {
        if (!validUser(p.configUserId())) {
          notImported(p, mam, sender, raw);
          return;
        }
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("removedByUserId", sender);
        content.put("removedUserId", p.configUserId());
        event(c, p, mam, sender, raw, "MEMBER_REMOVED", content);
      }
      case ROOM_NAME_CHANGED -> {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("changedBy", sender);
        content.put("name", p.text());
        event(c, p, mam, sender, raw, "ROOM_NAME_CHANGED", content);
      }
      case ROOM_DESCRIPTION_CHANGED -> {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("changedBy", sender);
        content.put("description", p.text());
        event(c, p, mam, sender, raw, "ROOM_DESCRIPTION_CHANGED", content);
      }
      case ROOM_PICTURE_UPDATED -> {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("updatedBy", sender);
        event(c, p, mam, sender, raw, "ROOM_PICTURE_UPDATED", content);
      }
      case ROOM_PICTURE_DELETED -> {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("deletedBy", sender);
        event(c, p, mam, sender, raw, "ROOM_PICTURE_DELETED", content);
      }
      case ROOM_HISTORY_CLEARED -> {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("clearedBy", sender);
        event(c, p, mam, sender, raw, "ROOM_HISTORY_CLEARED", content);
      }
      case MEETING_STARTED, MEETING_ENDED, MEETING_DECLINED -> meetingLine(c, p, mam, sender, raw);
      case PINNED, UNPINNED -> {
        OptionalLong ref = StanzaIds.decode(p.pinRef());
        if (p.kind() == Kind.PINNED && ref.isPresent()) {
          pinTimes.put(PinImporter.pinKey(ref.getAsLong(), sender), raw);
        }
        Row target = ref.isPresent() ? rows.resolveStanza(ref.getAsLong()) : null;
        if (target == null || target.outcome() != RoomRows.MESSAGE) {
          notImported(p, mam, sender, raw);
          return;
        }
        boolean pinned = p.kind() == Kind.PINNED;
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put(pinned ? "pinnedBy" : "unpinnedBy", sender);
        content.put("messageId", target.itemId());
        event(c, p, mam, sender, raw, pinned ? "MESSAGE_PINNED" : "MESSAGE_UNPINNED", content);
      }
      case EDIT -> {
        OptionalLong ref = StanzaIds.decode(p.applyToRef());
        if (ref.isEmpty()) {
          notImported(p, mam, sender, raw);
          return;
        }
        fastenings.stageEdit(mam, ref.getAsLong(), sender, p.text(), raw);
        rows.record(p.rootId(), Row.edit(mam, sender, ref.getAsLong(), raw));
      }
      case REACTION -> {
        OptionalLong ref = StanzaIds.decode(p.applyToRef());
        if (ref.isPresent()) {
          fastenings.stageReaction(mam, ref.getAsLong(), sender, p.text(), raw);
        }
        notImported(p, mam, sender, raw);
      }
      default -> notImported(p, mam, sender, raw);
    }
  }

  private void meetingLine(Context c, ParsedRow p, long mam, String sender, LocalDateTime raw)
      throws SQLException {
    String meetingId = c.room().meetingId();
    if (!c.oneToOne() || meetingId == null) {
      notImported(p, mam, sender, raw);
      return;
    }
    ObjectNode content = JsonNodeFactory.instance.objectNode();
    String type;
    switch (p.kind()) {
      case MEETING_STARTED -> {
        type = "MEETING_STARTED";
        content.put("startedBy", sender);
        content.put("meetingId", meetingId);
      }
      case MEETING_DECLINED -> {
        type = "MEETING_DECLINED";
        content.put("declinedBy", sender);
        content.put("meetingId", meetingId);
      }
      default -> {
        if (p.durationSec() == null) {
          notImported(p, mam, sender, raw);
          return;
        }
        type = "MEETING_ENDED";
        long startedAt =
            p.startedAtMillis() != null
                ? p.startedAtMillis()
                : raw.atZone(zone).toInstant().toEpochMilli() - p.durationSec() * 1000;
        content.put("endedBy", sender);
        content.put("meetingId", meetingId);
        content.put("startedAt", startedAt);
        content.put("durationSec", p.durationSec());
      }
    }
    event(c, p, mam, sender, raw, type, content);
  }

  private void attachmentOrForward(
      Context c, ParsedRow p, long mam, String sender, LocalDateTime raw) throws SQLException {
    String id = ids.of(LegacyIds.MESSAGE, c.room().id(), mam);
    LocalDateTime t = c.clock().next(raw);
    boolean forward = p.kind() == Kind.FORWARD;
    FileLinker.AttachmentDecision decision =
        !forward || p.attachmentId() != null
            ? files.onAttachment(id, p.attachmentId(), p.area())
            : new FileLinker.AttachmentDecision(null, false);
    if (decision.placeholder()) {
      writer.addMessage(mam, TimelineWriter.Message.placeholder(id, sender, t));
      rows.record(p.rootId(), Row.message(mam, sender, id, t, raw, true));
      return;
    }
    TimelineWriter.Message message =
        forward
            ? TimelineWriter.Message.forward(
                id,
                sender,
                p.text(),
                p.forwardFrom(),
                local(p.forwardStamp(), EPOCH),
                decision.attachmentId(),
                t)
            : TimelineWriter.Message.attachment(
                id, sender, p.text(), replyTo(p), decision.attachmentId(), t);
    writer.addMessage(mam, message);
    rows.record(p.rootId(), Row.message(mam, sender, id, t, raw, false));
  }

  private void event(
      Context c,
      ParsedRow p,
      long mam,
      String sender,
      LocalDateTime raw,
      String type,
      ObjectNode content)
      throws SQLException {
    String id = ids.of(LegacyIds.SYSTEM_EVENT, c.room().id(), mam);
    LocalDateTime t = c.clock().next(raw);
    writer.addEvent(mam, id, type, sender, content.toString(), t);
    rows.record(p.rootId(), Row.event(mam, sender, id, t, raw));
  }

  private void notImported(ParsedRow p, long mam, String sender, LocalDateTime raw)
      throws SQLException {
    rows.record(p.rootId(), Row.notImported(mam, sender, raw));
  }

  private String replyTo(ParsedRow p) throws SQLException {
    String ref = p.replyRef();
    if (ref == null || ref.isEmpty()) {
      return null;
    }
    OptionalLong stanza = StanzaIds.decode(ref);
    if (stanza.isEmpty()) {
      return null;
    }
    Row row = rows.resolveStanza(stanza.getAsLong());
    return row != null && row.outcome() == RoomRows.MESSAGE ? row.itemId() : null;
  }

  private LocalDateTime rawTime(long mam) {
    if (mam <= 0) {
      return null;
    }
    try {
      LocalDateTime time = MamTime.millis(mam, zone);
      return MamTime.inRange(time) ? time : null;
    } catch (DateTimeException e) {
      return null;
    }
  }

  private LocalDateTime local(Instant instant, LocalDateTime fallback) {
    if (instant == null) {
      return fallback;
    }
    try {
      LocalDateTime time = LocalDateTime.ofInstant(instant, zone);
      return MamTime.inRange(time) ? time : fallback;
    } catch (DateTimeException e) {
      return fallback;
    }
  }

  private static boolean validUser(String id) {
    return CanonicalUuid.matches(id);
  }
}
