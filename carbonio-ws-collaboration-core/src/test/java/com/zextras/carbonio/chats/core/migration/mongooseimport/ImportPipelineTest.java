// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.End;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.Entry;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.Source;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.Start;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.Target;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class ImportPipelineTest {

  private static final String READER = "mongoose-import-reader";

  private static ImportContext context() {
    return new ImportContext(Instant.now());
  }

  private static TargetRoom room(String id) {
    return new TargetRoom(id, "GROUP", null, "creator", null);
  }

  private static Entry entry(long mam, int weight) {
    return new Entry(mam, "srv", "user" + mam, ParsedRow.skip(), weight);
  }

  private static boolean readerAlive() {
    return Thread.getAllStackTraces().keySet().stream()
        .anyMatch(t -> READER.equals(t.getName()) && t.isAlive());
  }

  private static void awaitReaderGone() throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (readerAlive() && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertFalse(readerAlive());
  }

  private static class RecordingTarget implements Target {
    final List<String> events = new ArrayList<>();
    String room;
    long mam;

    @Override
    public void beginRoom(
        TargetRoom r,
        long floor,
        List<PinImporter.Pin> pins,
        Map<String, MarkerTranscriber.Marker> markers) {
      room = r.id();
      events.add("begin:" + r.id() + ":" + floor);
    }

    @Override
    public void handle(long m, String server, String user, ParsedRow parsed) throws SQLException {
      mam = m;
      events.add("handle:" + m);
    }

    @Override
    public void endRoom() {
      events.add("end");
      mam = 0;
    }

    @Override
    public String currentRoomId() {
      return room;
    }

    @Override
    public long failedMam() {
      return mam;
    }
  }

  private static class ScriptedSource implements Source {
    volatile long mam;

    @Override
    public void read(TargetRoom r, RoomReader.Sink sink) throws SQLException, InterruptedException {
      sink.put(new Start(r, 0, List.of(), Map.of()));
      for (long i = 1; i <= 3; i++) {
        mam = i;
        sink.put(entry(i, 1));
      }
      mam = 0;
      sink.put(new End());
    }

    @Override
    public long currentMam() {
      return mam;
    }
  }

  @Test
  @DisplayName("Consumes the items in the order they were read")
  void inOrder() throws Exception {
    RecordingTarget target = new RecordingTarget();
    ImportPipeline.run(List.of(room("a"), room("b")), new ScriptedSource(), target, context());
    assertEquals(
        List.of(
            "begin:a:0",
            "handle:1",
            "handle:2",
            "handle:3",
            "end",
            "begin:b:0",
            "handle:1",
            "handle:2",
            "handle:3",
            "end"),
        target.events);
    awaitReaderGone();
  }

  @Test
  @DisplayName("Completes with no rooms")
  void noRooms() throws Exception {
    RecordingTarget target = new RecordingTarget();
    ImportPipeline.run(List.of(), new ScriptedSource(), target, context());
    assertTrue(target.events.isEmpty());
    awaitReaderGone();
  }

  @Test
  @DisplayName("A reader SQLException fails the import with room, mam and SQLState only")
  void readerSqlException() throws Exception {
    ScriptedSource source =
        new ScriptedSource() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink)
              throws SQLException, InterruptedException {
            sink.put(new Start(r, 0, List.of(), Map.of()));
            mam = 42;
            throw new SQLException("secret message text", "08006");
          }
        };
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () ->
                ImportPipeline.run(List.of(room("r1")), source, new RecordingTarget(), context()));
    assertEquals("MongooseIM import failed (room r1, mam 42, SQLState 08006)", e.getMessage());
    assertFalse(e.getMessage().contains("secret"));
    assertEquals(null, e.getCause());
    awaitReaderGone();
  }

  @Test
  @DisplayName("A reader Error fails the import as an exception so Flyway rolls back")
  void readerError() throws Exception {
    OutOfMemoryError oom = new OutOfMemoryError("secret");
    Source source =
        new ScriptedSource() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink) {
            throw oom;
          }
        };
    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                ImportPipeline.run(List.of(room("r1")), source, new RecordingTarget(), context()));
    assertSame(oom, thrown.getCause());
    assertFalse(thrown.getMessage().contains("secret"));
    awaitReaderGone();
  }

  @Test
  @DisplayName("A consumer Error fails the import as an exception so Flyway rolls back")
  void consumerError() throws Exception {
    OutOfMemoryError oom = new OutOfMemoryError("secret");
    RecordingTarget target =
        new RecordingTarget() {
          @Override
          public void handle(long m, String s, String u, ParsedRow p) {
            throw oom;
          }
        };
    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () -> ImportPipeline.run(List.of(room("r1")), new ScriptedSource(), target, context()));
    assertSame(oom, thrown.getCause());
    assertFalse(thrown.getMessage().contains("secret"));
    awaitReaderGone();
  }

  @Test
  @DisplayName(
      "A RuntimeException on one entry is counted, the row is discarded and the rest is read")
  void rowRuntimeExceptionIsSkipped() throws Exception {
    List<Long> discarded = new ArrayList<>();
    RecordingTarget target =
        new RecordingTarget() {
          @Override
          public void handle(long m, String s, String u, ParsedRow p) throws SQLException {
            if (m == 2) {
              throw new IllegalStateException("broken row");
            }
            super.handle(m, s, u, p);
          }

          @Override
          public void discardRow(long m) {
            discarded.add(m);
          }
        };
    ImportContext context = context();

    ImportPipeline.run(List.of(room("r1")), new ScriptedSource(), target, context);

    assertEquals(List.of("begin:r1:0", "handle:1", "handle:3", "end"), target.events);
    assertEquals(List.of(2L), discarded);
    assertEquals(1, context.of(SkipReason.ROW_IMPORT_ERROR));
    assertEquals(3, context.rowsRead());
    assertTrue(context.summary().contains("row_import_error=1 (e.g. r1/2)"));
  }

  @Test
  @DisplayName("A reader RuntimeException propagates as is")
  void readerRuntimeException() throws Exception {
    IllegalArgumentException failure = new IllegalArgumentException("bad");
    Source source =
        new ScriptedSource() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink) {
            throw failure;
          }
        };
    assertSame(
        failure,
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ImportPipeline.run(List.of(room("r1")), source, new RecordingTarget(), context())));
    awaitReaderGone();
  }

  @Test
  @DisplayName("A consumer failure stops a reader that would never end")
  void consumerFailureStopsReader() throws Exception {
    Source endless =
        new ScriptedSource() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink)
              throws SQLException, InterruptedException {
            sink.put(new Start(r, 0, List.of(), Map.of()));
            for (long i = 1; ; i++) {
              sink.put(entry(i, 1));
            }
          }
        };
    RecordingTarget target =
        new RecordingTarget() {
          @Override
          public void handle(long m, String s, String u, ParsedRow p) throws SQLException {
            mam = m;
            if (m == 5) {
              throw new SQLException("hidden", "23505");
            }
          }
        };
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ImportPipeline.run(List.of(room("r9")), endless, target, context(), 4));
    assertEquals("MongooseIM import failed (room r9, mam 5, SQLState 23505)", e.getMessage());
    awaitReaderGone();
  }

  @Test
  @DisplayName("A consumer failure stops a reader blocked on the budget")
  void consumerFailureWithBlockedReader() throws Exception {
    Source source =
        new ScriptedSource() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink)
              throws SQLException, InterruptedException {
            sink.put(new Start(r, 0, List.of(), Map.of()));
            sink.put(entry(1, 4));
            sink.put(entry(2, 4));
            sink.put(entry(3, 4));
          }
        };
    RecordingTarget target =
        new RecordingTarget() {
          @Override
          public void handle(long m, String s, String u, ParsedRow p) throws SQLException {
            throw new SQLException("consumer failed", "08006");
          }
        };
    assertThrows(
        IllegalStateException.class,
        () -> ImportPipeline.run(List.of(room("r1")), source, target, context(), 4));
    awaitReaderGone();
  }

  @Test
  @DisplayName("A reader that dies without reporting does not hang the consumer")
  void silentReaderDeath() throws Exception {
    Source source =
        new Source() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink) throws SQLException {
            throw new SQLException("x", "08006");
          }

          @Override
          public long currentMam() {
            throw new IllegalStateException("cannot report");
          }
        };
    assertTimeoutPreemptivelyThrows(source);
    awaitReaderGone();
  }

  private static void assertTimeoutPreemptivelyThrows(Source source) {
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
        java.time.Duration.ofSeconds(15),
        () ->
            assertThrows(
                IllegalStateException.class,
                () ->
                    ImportPipeline.run(
                        List.of(room("r1")), source, new RecordingTarget(), context())));
  }

  @Test
  @DisplayName("The byte budget bounds the queued items")
  void budgetBoundsQueue() throws Exception {
    AtomicInteger accepted = new AtomicInteger();
    CountDownLatch consumerEntered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    int total = 20;
    Source source =
        new ScriptedSource() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink)
              throws SQLException, InterruptedException {
            sink.put(new Start(r, 0, List.of(), Map.of()));
            for (long i = 1; i <= total; i++) {
              sink.put(entry(i, 4));
              accepted.incrementAndGet();
            }
            sink.put(new End());
          }
        };
    RecordingTarget target =
        new RecordingTarget() {
          @Override
          public void handle(long m, String s, String u, ParsedRow p) throws SQLException {
            consumerEntered.countDown();
            try {
              release.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            super.handle(m, s, u, p);
          }
        };
    Thread runner =
        new Thread(() -> ImportPipeline.run(List.of(room("r1")), source, target, context(), 8));
    runner.start();
    assertTrue(consumerEntered.await(10, TimeUnit.SECONDS));
    Thread.sleep(1500);
    int whileBlocked = accepted.get();
    release.countDown();
    runner.join(TimeUnit.SECONDS.toMillis(30));
    assertFalse(runner.isAlive());
    assertTrue(whileBlocked >= 1 && whileBlocked <= 3, "accepted while blocked: " + whileBlocked);
    assertEquals(total, accepted.get());
    assertEquals(total + 2, target.events.size());
  }

  @Test
  @DisplayName("An item heavier than the whole budget is still accepted")
  void oversizedItem() throws Exception {
    Source source =
        new ScriptedSource() {
          @Override
          public void read(TargetRoom r, RoomReader.Sink sink)
              throws SQLException, InterruptedException {
            sink.put(new Start(r, 0, List.of(), Map.of()));
            sink.put(entry(1, 1_000_000));
            sink.put(entry(2, 1_000_000));
            sink.put(new End());
          }
        };
    RecordingTarget target = new RecordingTarget();
    ImportPipeline.run(List.of(room("r1")), source, target, context(), 8);
    assertEquals(List.of("begin:r1:0", "handle:1", "handle:2", "end"), target.events);
    awaitReaderGone();
  }
}
