// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration;

import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.systemd.SystemdNotify;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;

public class MigrationKeepAlive {

  static final String THREAD_NAME = "WSC-BootKeepAlive";
  static final String EXTEND_ENV = "WSC_MIGRATION_EXTEND_SECONDS";
  static final String STALL_ENV = "WSC_MIGRATION_STALL_SECONDS";
  static final Duration DEFAULT_WINDOW = Duration.ofSeconds(120);
  static final Duration DEFAULT_STALL = Duration.ofMinutes(10);
  static final Duration MAX_PERIOD = Duration.ofSeconds(30);
  static final Duration STOP_WAIT = Duration.ofSeconds(5);

  interface DatabaseProbe {
    boolean isBackendActive() throws Exception;
  }

  private final ScheduledExecutorService scheduler;
  private final LongSupplier clock;
  private final LongSupplier lastProgress;
  private final Supplier<String> status;
  private final DatabaseProbe probe;
  private final BiConsumer<Duration, String> notifier;
  private final Duration window;
  private final Duration period;
  private final Duration stallLimit;

  private long lastSeenProgress;
  private long lastProgressAt;
  private boolean stallLogged;
  private boolean probeFailureLogged;
  private final Object notifyLock = new Object();
  private boolean stopped;

  MigrationKeepAlive(
      ScheduledExecutorService scheduler,
      LongSupplier clock,
      LongSupplier lastProgress,
      Supplier<String> status,
      DatabaseProbe probe,
      BiConsumer<Duration, String> notifier,
      Duration window,
      Duration stallLimit) {
    this.scheduler = scheduler;
    this.clock = clock;
    this.lastProgress = lastProgress;
    this.status = status;
    this.probe = probe;
    this.notifier = notifier;
    this.window = window;
    this.period = window.dividedBy(3).compareTo(MAX_PERIOD) < 0 ? window.dividedBy(3) : MAX_PERIOD;
    this.stallLimit = stallLimit;
  }

  public static void migrate(Flyway flyway, HikariDataSource dataSource) {
    MigrationKeepAlive keepAlive =
        systemd(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
    keepAlive.start();
    try {
      flyway.migrate();
    } finally {
      keepAlive.stop();
    }
  }

  static MigrationKeepAlive systemd(String jdbcUrl, String user, String password) {
    return new MigrationKeepAlive(
        Executors.newSingleThreadScheduledExecutor(MigrationKeepAlive::newDaemonThread),
        System::nanoTime,
        MigrationProgress::lastProgressNanos,
        MigrationProgress::status,
        () -> isBackendActive(jdbcUrl, user, password, MigrationProgress.backendPid()),
        SystemdNotify::extendTimeout,
        seconds(EXTEND_ENV, DEFAULT_WINDOW),
        seconds(STALL_ENV, DEFAULT_STALL));
  }

  static Duration seconds(String env, Duration fallback) {
    String value = System.getenv(env);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      long seconds = Long.parseLong(value.trim());
      return seconds > 0 ? Duration.ofSeconds(seconds) : fallback;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  static Thread newDaemonThread(Runnable runnable) {
    Thread thread = new Thread(runnable, THREAD_NAME);
    thread.setDaemon(true);
    return thread;
  }

  static boolean isBackendActive(String jdbcUrl, String user, String password, long pid)
      throws Exception {
    if (pid < 0) {
      return false;
    }
    Properties props = new Properties();
    props.setProperty("user", user);
    props.setProperty("password", password);
    props.setProperty("connectTimeout", "5");
    props.setProperty("socketTimeout", "10");
    props.setProperty("ApplicationName", "ws-collaboration-keepalive");
    try (Connection connection = DriverManager.getConnection(jdbcUrl, props);
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT 1 FROM pg_stat_activity WHERE pid = ? AND state = 'active'"
                    + " AND wait_event_type IS DISTINCT FROM 'Lock'")) {
      statement.setLong(1, pid);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    }
  }

  public void start() {
    lastSeenProgress = lastProgress.getAsLong();
    lastProgressAt = clock.getAsLong();
    scheduler.scheduleAtFixedRate(this::tick, 0, period.toMillis(), TimeUnit.MILLISECONDS);
  }

  public void stop() {
    synchronized (notifyLock) {
      stopped = true;
    }
    scheduler.shutdownNow();
    try {
      scheduler.awaitTermination(STOP_WAIT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void notifyIfRunning() {
    synchronized (notifyLock) {
      if (!stopped) {
        notifier.accept(window, status.get());
      }
    }
  }

  void tick() {
    try {
      doTick();
    } catch (Throwable t) {
      ChatsLogger.warn("Systemd timeout extension failed: " + t.getMessage());
    }
  }

  private void doTick() {
    long now = clock.getAsLong();
    long progress = lastProgress.getAsLong();
    boolean progressed = progress != lastSeenProgress;
    lastSeenProgress = progress;
    if (progressed || backendActive()) {
      lastProgressAt = now;
      stallLogged = false;
      notifyIfRunning();
      return;
    }
    Duration idle = Duration.ofNanos(now - lastProgressAt);
    if (idle.compareTo(stallLimit) > 0) {
      if (!stallLogged) {
        stallLogged = true;
        ChatsLogger.warn(
            "Migration made no progress for "
                + idle.toSeconds()
                + "s; letting systemd's start timeout expire");
      }
      return;
    }
    notifyIfRunning();
  }

  private boolean backendActive() {
    try {
      return probe.isBackendActive();
    } catch (Throwable t) {
      if (!probeFailureLogged) {
        probeFailureLogged = true;
        ChatsLogger.warn("Migration backend activity check failed: " + t.getMessage());
      }
      return false;
    }
  }
}
