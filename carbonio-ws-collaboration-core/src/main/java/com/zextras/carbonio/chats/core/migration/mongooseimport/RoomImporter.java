// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ParsedRow.Kind;
import com.zextras.carbonio.chats.core.migration.mongooseimport.RoomRows.Row;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

public final class RoomImporter implements AutoCloseable, ImportPipeline.Target {

  public record Counts(int rooms, int messages, int events, int edits, int reactions) {}

  private record Context(TargetRoom room, boolean temporary, long floor, RoomClock clock) {
    boolean oneToOne() {
      return "ONE_TO_ONE".equals(room.type());
    }
  }

  private final LegacyIds ids;
  private final FileLinker files;
  private final PinImporter pins;
  private final MarkerTranscriber markers;
  private final RoomRows rows;
  private final Fastenings fastenings;
  private final TimelineWriter writer;
  private static final long MAX_MEETING_SECONDS = 31L * 24 * 3600;

  private final ImportContext stats;
  private final Set<String> roomIds;
  private final Map<String, Instant> pinTimes = new HashMap<>();
  private PinImporter.PinLine lastPinLine;
  private List<PinImporter.Pin> roomPins = List.of();
  private Map<String, MarkerTranscriber.Marker> roomMarkers = Map.of();

  private int rooms;
  private int edits;
  private int reactions;
  private Context context;
  private boolean archived;
  private long currentMam;

  public RoomImporter(
      Connection target,
      LegacyIds ids,
      FileLinker files,
      PinImporter pins,
      MarkerTranscriber markers,
      ImportContext stats,
      Set<String> roomIds)
      throws SQLException {
    this.stats = stats;
    this.roomIds = roomIds;
    this.ids = ids;
    this.files = files;
    this.pins = pins;
    this.markers = markers;
    this.rows = new RoomRows(target);
    this.fastenings = new Fastenings(target);
    this.writer = new TimelineWriter(target, files, stats);
  }

  public Counts counts() {
    return new Counts(rooms, writer.messages(), writer.events(), edits, reactions);
  }

  @Override
  public long written() {
    return writer.messages() + writer.events();
  }

  @Override
  public void discardRow(long mam) {
    if (context == null) {
      return;
    }
    files.discard(ids.of(LegacyIds.MESSAGE, context.room().id(), mam));
    if (!rows.knows(mam)) {
      rows.record(null, Row.notImported(mam, null, Instant.EPOCH));
    }
  }

  @Override
  public String currentRoomId() {
    if (writer.failedRoom() != null) {
      return writer.failedRoom();
    }
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
  public void beginRoom(
      TargetRoom room,
      long floor,
      List<PinImporter.Pin> pinRows,
      Map<String, MarkerTranscriber.Marker> markerRows)
      throws SQLException {
    writer.beginRoom(room.id());
    String roomId = room.id();
    rows.beginRoom(
        roomId,
        mam -> ids.of(LegacyIds.MESSAGE, roomId, mam),
        () -> {
          writer.flush();
          fastenings.flushEdits();
        },
        markerRows);
    roomPins = pinRows;
    roomMarkers = markerRows;
    pinTimes.clear();
    lastPinLine = null;
    files.beginRoom(room.id());
    context = new Context(room, "TEMPORARY".equals(room.type()), floor, new RoomClock());
    archived = false;
    currentMam = 0;
  }

  @Override
  public void endRoom() throws SQLException {
    currentMam = 0;
    if (!archived) {
      return;
    }
    TargetRoom room = context.room();
    writer.roomCreated(room, ids.of(LegacyIds.ROOM_CREATED, room.id(), 0), context.clock());
    if (!roomPins.isEmpty()) {
      writer.flush();
    }
    pins.importRoomPin(room, rows, roomPins, pinTimes, lastPinLine);
    markers.endRoom(room, rows, roomMarkers);
    rooms++;
  }

  public void finish() throws SQLException {
    writer.flush();
    long start = System.nanoTime();
    ChatsLogger.info(RoomImporter.class, "MongooseIM import: flushing read markers");
    markers.flush();
    ChatsLogger.info(
        RoomImporter.class,
        "MongooseIM import: read markers flushed in " + millisSince(start) + " ms");
    start = System.nanoTime();
    ChatsLogger.info(RoomImporter.class, "MongooseIM import: folding edits and reactions");
    Fastenings.Counts folded = fastenings.fold();
    ChatsLogger.info(
        RoomImporter.class,
        "MongooseIM import: edits and reactions folded in " + millisSince(start) + " ms");
    edits += folded.edits();
    reactions += folded.reactions();
    stats.count(SkipReason.EDIT_DROPPED, folded.droppedEdits());
    stats.count(SkipReason.REACTION_DROPPED, folded.droppedReactions());
  }

  private static long millisSince(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }

  @Override
  public void handle(long mam, String server, String user, ParsedRow p) throws SQLException {
    Context c = context;
    currentMam = mam;
    archived = true;
    String roomId = c.room().id();
    for (SkipReason note : p.notes()) {
      stats.count(note, roomId, mam);
    }
    if (p.skipReason() != null) {
      stats.count(p.skipReason(), roomId, mam);
    }
    Instant raw = rawTime(mam, roomId);
    if (raw == null) {
      if (p.skipReason() == null) {
        stats.count(SkipReason.INVALID_MAM_TIME, roomId, mam);
      }
      rows.record(p.rootId(), Row.notImported(mam, null, Instant.EPOCH));
      return;
    }
    String sender = "carbonio".equals(server) && validUser(user) ? user : null;
    if (sender == null && p.skipReason() == null && mam >= c.floor()) {
      stats.count(SkipReason.INVALID_SENDER, roomId, mam);
    }
    if (mam < c.floor() || sender == null) {
      rows.record(p.rootId(), Row.notImported(mam, sender, raw));
      return;
    }
    switch (p.kind()) {
      case TEXT -> {
        String id = ids.of(LegacyIds.MESSAGE, roomId, mam);
        Instant t = c.clock().next(raw);
        writer.addMessage(
            mam, TimelineWriter.Message.text(id, sender, p.text(), replyTo(c, p, mam), t));
        rows.record(p.rootId(), Row.message(mam, sender, id, t, raw, false));
      }
      case TOMBSTONE -> {
        String id = ids.of(LegacyIds.MESSAGE, roomId, mam);
        Instant t = c.clock().next(raw);
        String attachment = files.onTombstone(mam, id);
        Instant deletedAt = deletionTime(p.retractedStamp(), t, roomId, mam);
        writer.addMessage(
            mam, TimelineWriter.Message.tombstone(id, sender, attachment, deletedAt, t));
        rows.record(p.rootId(), Row.message(mam, sender, id, t, raw, true));
      }
      case FORWARD, ATTACHMENT -> attachmentOrForward(c, p, mam, sender, raw);
      case MEMBER_ADDED -> {
        String member = lower(p.configUserId());
        if (c.oneToOne() || !validUser(member)) {
          notImported(
              p,
              mam,
              sender,
              raw,
              c.oneToOne() ? SkipReason.MEMBER_EVENT_ONE_TO_ONE : SkipReason.INVALID_MEMBER_ID);
          return;
        }
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("addedByUserId", sender);
        content.putArray("addedUserIds").add(member);
        event(c, p, mam, sender, raw, "MEMBER_ADDED", content);
      }
      case MEMBER_REMOVED -> {
        String member = lower(p.configUserId());
        if (!validUser(member)) {
          notImported(p, mam, sender, raw, SkipReason.INVALID_MEMBER_ID);
          return;
        }
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("removedByUserId", sender);
        content.put("removedUserId", member);
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
        boolean message = target != null && target.outcome() == RoomRows.MESSAGE;
        lastPinLine =
            new PinImporter.PinLine(
                p.kind() == Kind.PINNED,
                ref.orElse(0),
                message ? target.itemId() : null,
                sender,
                raw);
        if (!message) {
          notImported(p, mam, sender, raw, SkipReason.UNRESOLVED_PIN);
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
          notImported(p, mam, sender, raw, SkipReason.UNRESOLVED_EDIT);
          return;
        }
        fastenings.stageEdit(
            roomId,
            mam,
            ref.getAsLong(),
            ids.of(LegacyIds.MESSAGE, roomId, ref.getAsLong()),
            sender,
            p.text(),
            raw);
        rows.record(p.rootId(), Row.edit(mam, sender, ref.getAsLong(), raw));
      }
      case REACTION -> {
        OptionalLong ref = StanzaIds.decode(p.applyToRef());
        if (ref.isPresent()) {
          fastenings.stageReaction(
              mam,
              ref.getAsLong(),
              ids.of(LegacyIds.MESSAGE, roomId, ref.getAsLong()),
              sender,
              p.text(),
              raw);
          notImported(p, mam, sender, raw);
        } else {
          notImported(p, mam, sender, raw, SkipReason.UNRESOLVED_REACTION);
        }
      }
      default -> notImported(p, mam, sender, raw);
    }
  }

  private void meetingLine(Context c, ParsedRow p, long mam, String sender, Instant raw)
      throws SQLException {
    String meetingId = c.room().meetingId();
    if (!c.oneToOne() || meetingId == null) {
      notImported(p, mam, sender, raw, SkipReason.MEETING_LINE_SKIPPED);
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
        Long duration = p.durationSec();
        if (duration != null && (duration < 0 || duration > MAX_MEETING_SECONDS)) {
          stats.count(SkipReason.MEETING_DURATION_DROPPED, c.room().id(), mam);
          duration = null;
        }
        Long started = p.startedAtMillis();
        if (started != null && (started < 0 || started > stats.t0().toEpochMilli())) {
          stats.count(SkipReason.MEETING_STARTED_AT_DROPPED, c.room().id(), mam);
          started = null;
        }
        if (duration == null) {
          notImported(p, mam, sender, raw, SkipReason.MEETING_LINE_SKIPPED);
          return;
        }
        type = "MEETING_ENDED";
        long startedAt = started != null ? started : raw.toEpochMilli() - duration * 1000;
        content.put("endedBy", sender);
        content.put("meetingId", meetingId);
        content.put("startedAt", startedAt);
        content.put("durationSec", duration);
      }
    }
    event(c, p, mam, sender, raw, type, content);
  }

  private void attachmentOrForward(Context c, ParsedRow p, long mam, String sender, Instant raw)
      throws SQLException {
    String id = ids.of(LegacyIds.MESSAGE, c.room().id(), mam);
    Instant t = c.clock().next(raw);
    boolean forward =
        p.kind() == Kind.FORWARD && p.forwardFrom() != null && !roomIds.contains(p.forwardFrom());
    if (p.kind() == Kind.FORWARD && !forward) {
      stats.count(SkipReason.FORWARD_AS_PLAIN, c.room().id(), mam);
    }
    FileLinker.AttachmentDecision decision =
        p.kind() == Kind.ATTACHMENT || p.attachmentId() != null
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
                forwardTime(p.forwardStamp(), c.room().id(), mam),
                decision.attachmentId(),
                t)
            : TimelineWriter.Message.attachment(
                id,
                sender,
                p.text(),
                p.kind() == Kind.FORWARD ? null : replyTo(c, p, mam),
                decision.attachmentId(),
                t);
    writer.addMessage(mam, message);
    rows.record(p.rootId(), Row.message(mam, sender, id, t, raw, false));
  }

  private void event(
      Context c, ParsedRow p, long mam, String sender, Instant raw, String type, ObjectNode content)
      throws SQLException {
    String id = ids.of(LegacyIds.SYSTEM_EVENT, c.room().id(), mam);
    Instant t = c.clock().next(raw);
    writer.addEvent(mam, id, type, sender, content.toString(), t);
    rows.record(p.rootId(), Row.event(mam, sender, id, t, raw));
  }

  private void notImported(ParsedRow p, long mam, String sender, Instant raw) throws SQLException {
    rows.record(p.rootId(), Row.notImported(mam, sender, raw));
  }

  private void notImported(ParsedRow p, long mam, String sender, Instant raw, SkipReason reason)
      throws SQLException {
    stats.count(reason, context.room().id(), mam);
    notImported(p, mam, sender, raw);
  }

  private String replyTo(Context c, ParsedRow p, long mam) throws SQLException {
    String ref = p.replyRef();
    if (ref == null || ref.isEmpty()) {
      return null;
    }
    OptionalLong stanza = StanzaIds.decode(ref);
    Row row = stanza.isEmpty() ? null : rows.resolveStanza(stanza.getAsLong());
    if (row == null || row.outcome() != RoomRows.MESSAGE) {
      stats.count(SkipReason.UNRESOLVED_REPLY, c.room().id(), mam);
      return null;
    }
    return row.itemId();
  }

  private Instant rawTime(long mam, String roomId) {
    if (mam <= 0) {
      return null;
    }
    Instant time = MamTime.instant(mam);
    if (!MamTime.inRange(time)) {
      return null;
    }
    if (time.isAfter(stats.t0())) {
      stats.count(SkipReason.TIME_CLAMPED, roomId, mam);
      return stats.t0();
    }
    return time;
  }

  private Instant deletionTime(Instant retracted, Instant createdAt, String roomId, long mam) {
    Instant deletedAt = stamp(retracted, createdAt);
    if (deletedAt.isAfter(stats.t0())) {
      stats.count(SkipReason.STAMP_CLAMPED, roomId, mam);
      deletedAt = stats.t0();
    }
    if (deletedAt.isBefore(createdAt)) {
      stats.count(SkipReason.RETRACT_STAMP_RAISED, roomId, mam);
      deletedAt = createdAt;
    }
    return deletedAt;
  }

  private Instant forwardTime(Instant forwarded, String roomId, long mam) {
    Instant time = stamp(forwarded, null);
    if (time == null) {
      stats.count(SkipReason.FORWARD_STAMP_MISSING, roomId, mam);
      return null;
    }
    if (time.isAfter(stats.t0())) {
      stats.count(SkipReason.STAMP_CLAMPED, roomId, mam);
      return stats.t0();
    }
    return time;
  }

  private static String lower(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }

  static Instant stamp(Instant instant, Instant fallback) {
    if (instant == null) {
      return fallback;
    }
    Instant rounded = MamTime.roundToMicros(instant);
    return MamTime.inRange(rounded) ? rounded : fallback;
  }

  private static boolean validUser(String id) {
    return CanonicalUuid.matches(id);
  }
}
