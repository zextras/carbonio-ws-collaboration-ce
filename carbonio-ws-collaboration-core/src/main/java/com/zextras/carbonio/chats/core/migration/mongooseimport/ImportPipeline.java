// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// The reader thread owns the source connection, the calling thread owns the migration connection.
final class ImportPipeline {

  sealed interface Item permits Start, Entry, End, Done, Failed {
    default int weight() {
      return 0;
    }
  }

  record Start(TargetRoom room, long floor) implements Item {}

  record Entry(long mam, String server, String user, ParsedRow parsed, int weight)
      implements Item {}

  record End(List<PinImporter.Pin> pins, Map<String, MarkerTranscriber.Marker> markers)
      implements Item {}

  record Done() implements Item {}

  record Failed(Throwable cause) implements Item {}

  interface Source {
    void read(TargetRoom room, RoomReader.Sink sink) throws SQLException, InterruptedException;

    long currentMam();
  }

  interface Target {
    void beginRoom(TargetRoom room, long floor) throws SQLException;

    void handle(long mam, String server, String user, ParsedRow parsed) throws SQLException;

    void endRoom(List<PinImporter.Pin> pins, Map<String, MarkerTranscriber.Marker> markers)
        throws SQLException;

    String currentRoomId();

    long failedMam();
  }

  private static final int BUDGET_KIB = 16 * 1024;
  private static final long POLL_SECONDS = 1;
  private static final long JOIN_SECONDS = 60;

  private final BlockingQueue<Item> queue = new LinkedBlockingQueue<>();
  private final Semaphore budget;
  private final int budgetKib;
  private volatile boolean stopped;

  private ImportPipeline(int budgetKib) {
    this.budget = new Semaphore(budgetKib);
    this.budgetKib = budgetKib;
  }

  static void run(List<TargetRoom> rooms, Source reader, Target importer) {
    run(rooms, reader, importer, BUDGET_KIB);
  }

  static void run(List<TargetRoom> rooms, Source reader, Target importer, int budgetKib) {
    new ImportPipeline(budgetKib).execute(rooms, reader, importer);
  }

  private void execute(List<TargetRoom> rooms, Source reader, Target importer) {
    Thread thread = new Thread(() -> produce(rooms, reader), "mongoose-import-reader");
    thread.setDaemon(true);
    thread.start();
    try {
      consume(importer, thread);
    } catch (Error error) {
      throw new IllegalStateException(
          "MongooseIM import failed (" + error.getClass().getSimpleName() + ")", error);
    } finally {
      stopped = true;
      thread.interrupt();
      try {
        thread.join(TimeUnit.SECONDS.toMillis(JOIN_SECONDS));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void produce(List<TargetRoom> rooms, Source reader) {
    String roomId = null;
    try {
      for (TargetRoom room : rooms) {
        roomId = room.id();
        reader.read(room, this::put);
      }
      queue.add(new Done());
    } catch (SQLException e) {
      queue.add(new Failed(failure(roomId, reader.currentMam(), e)));
    } catch (Throwable t) {
      queue.add(new Failed(t));
    }
  }

  private void put(Item item) throws InterruptedException {
    int weight = cost(item);
    while (weight > 0 && !budget.tryAcquire(weight, POLL_SECONDS, TimeUnit.SECONDS)) {
      if (stopped) {
        throw new InterruptedException("import stopped");
      }
    }
    queue.add(item);
  }

  private void consume(Target importer, Thread reader) {
    while (true) {
      Item item = next(reader);
      budget.release(cost(item));
      try {
        switch (item) {
          case Start start -> importer.beginRoom(start.room(), start.floor());
          case Entry entry ->
              importer.handle(entry.mam(), entry.server(), entry.user(), entry.parsed());
          case End end -> importer.endRoom(end.pins(), end.markers());
          case Done done -> {
            return;
          }
          case Failed failed -> throw rethrow(failed.cause());
        }
      } catch (SQLException e) {
        throw failure(importer.currentRoomId(), importer.failedMam(), e);
      }
    }
  }

  private Item next(Thread reader) {
    try {
      while (true) {
        Item item = queue.poll(POLL_SECONDS, TimeUnit.SECONDS);
        if (item != null) {
          return item;
        }
        if (!reader.isAlive() && queue.isEmpty()) {
          throw new IllegalStateException("MongooseIM import failed: the source reader stopped");
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("MongooseIM import interrupted");
    }
  }

  private int cost(Item item) {
    return Math.min(item.weight(), budgetKib);
  }

  private static RuntimeException rethrow(Throwable cause) {
    if (cause instanceof Error error) {
      throw error;
    }
    if (cause instanceof RuntimeException runtime) {
      return runtime;
    }
    return new IllegalStateException("MongooseIM import failed in the source reader", cause);
  }

  private static IllegalStateException failure(String roomId, long mam, SQLException e) {
    return new IllegalStateException(
        "MongooseIM import failed (room "
            + roomId
            + ", mam "
            + mam
            + ", SQLState "
            + e.getSQLState()
            + ")");
  }
}
