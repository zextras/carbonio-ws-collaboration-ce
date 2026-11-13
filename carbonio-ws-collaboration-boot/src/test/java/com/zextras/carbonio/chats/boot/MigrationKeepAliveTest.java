// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MigrationKeepAliveTest {

  private static final Duration WINDOW = Duration.ofSeconds(120);
  private static final Duration STALL = Duration.ofMinutes(10);

  private static final class Fixture {
    final List<Object[]> scheduled = new ArrayList<>();
    final List<String> calls = new ArrayList<>();
    final List<String> notifications = new ArrayList<>();
    final List<Duration> windows = new ArrayList<>();
    final ScheduledExecutorService executor =
        (ScheduledExecutorService)
            Proxy.newProxyInstance(
                Fixture.class.getClassLoader(),
                new Class<?>[] {ScheduledExecutorService.class},
                (proxy, method, args) -> {
                  calls.add(method.getName());
                  if (method.getName().equals("scheduleAtFixedRate")) {
                    scheduled.add(args);
                  }
                  return null;
                });
    long now = 0;
    long progress = 0;
    String status = "status";
    int probeCalls = 0;
    MigrationKeepAlive.DatabaseProbe probe = () -> false;
    final MigrationKeepAlive keepAlive;

    Fixture() {
      this(WINDOW);
    }

    Fixture(Duration window) {
      keepAlive =
          new MigrationKeepAlive(
              executor,
              () -> now,
              () -> progress,
              () -> status,
              () -> {
                probeCalls++;
                return probe.isBackendActive();
              },
              (duration, text) -> {
                windows.add(duration);
                notifications.add(text);
              },
              window,
              STALL);
    }

    void advance(Duration duration) {
      now += duration.toNanos();
    }
  }

  @Test
  @DisplayName("start schedules the tick immediately and every 30 seconds")
  void start_schedulesAtFixedRate() {
    Fixture fixture = new Fixture();

    fixture.keepAlive.start();

    assertEquals(1, fixture.scheduled.size());
    assertEquals(0L, fixture.scheduled.get(0)[1]);
    assertEquals(30_000L, fixture.scheduled.get(0)[2]);
    assertEquals(TimeUnit.MILLISECONDS, fixture.scheduled.get(0)[3]);
  }

  @Test
  @DisplayName("a short window shortens the period to a third of it")
  void start_shortWindow_shortensPeriod() {
    Fixture fixture = new Fixture(Duration.ofSeconds(9));

    fixture.keepAlive.start();

    assertEquals(3_000L, fixture.scheduled.get(0)[2]);
  }

  @Test
  @DisplayName("progress since the previous tick extends the timeout with the latest status")
  void tick_progress_extends() {
    Fixture fixture = new Fixture();
    fixture.keepAlive.start();
    fixture.progress = 1;
    fixture.status = "Migrating 2.0.1";
    fixture.advance(Duration.ofSeconds(30));

    fixture.keepAlive.tick();

    assertEquals(List.of("Migrating 2.0.1"), fixture.notifications);
    assertEquals(List.of(WINDOW), fixture.windows);
  }

  @Test
  @DisplayName("no progress within the stall limit keeps extending")
  void tick_noProgressWithinStall_extends() {
    Fixture fixture = new Fixture();
    fixture.keepAlive.start();
    fixture.advance(Duration.ofMinutes(9));

    fixture.keepAlive.tick();
    fixture.advance(Duration.ofSeconds(30));
    fixture.keepAlive.tick();

    assertEquals(2, fixture.notifications.size());
  }

  @Test
  @DisplayName("no progress beyond the stall limit stops extending")
  void tick_noProgressBeyondStall_doesNotExtend() {
    Fixture fixture = new Fixture();
    fixture.keepAlive.start();
    fixture.advance(STALL.plusSeconds(1));

    fixture.keepAlive.tick();
    fixture.keepAlive.tick();

    assertTrue(fixture.notifications.isEmpty());
  }

  @Test
  @DisplayName("a stall is measured from the last progress, not from the start")
  void tick_stallMeasuredFromLastProgress() {
    Fixture fixture = new Fixture();
    fixture.keepAlive.start();
    fixture.advance(Duration.ofMinutes(8));
    fixture.progress = 1;
    fixture.keepAlive.tick();
    fixture.advance(Duration.ofMinutes(8));

    fixture.keepAlive.tick();

    assertEquals(2, fixture.notifications.size());
  }

  @Test
  @DisplayName("progress after a stall resumes extending")
  void tick_progressAfterStall_resumes() {
    Fixture fixture = new Fixture();
    fixture.keepAlive.start();
    fixture.advance(STALL.plusSeconds(1));
    fixture.keepAlive.tick();
    fixture.progress = 1;

    fixture.keepAlive.tick();

    assertEquals(1, fixture.notifications.size());
  }

  @Test
  @DisplayName("an active database backend counts as progress even beyond the stall limit")
  void tick_databaseActive_extends() {
    Fixture fixture = new Fixture();
    fixture.probe = () -> true;
    fixture.keepAlive.start();
    fixture.advance(STALL.multipliedBy(3));

    fixture.keepAlive.tick();
    fixture.advance(STALL.multipliedBy(3));
    fixture.keepAlive.tick();

    assertEquals(2, fixture.notifications.size());
  }

  @Test
  @DisplayName("a failing database check is no information: the stall limit still applies")
  void tick_databaseCheckFails_isNoInformation() {
    Fixture fixture = new Fixture();
    fixture.probe =
        () -> {
          throw new IllegalStateException("connection refused");
        };
    fixture.keepAlive.start();

    fixture.keepAlive.tick();
    fixture.advance(STALL.plusSeconds(1));
    fixture.keepAlive.tick();

    assertEquals(1, fixture.notifications.size());
    assertEquals(2, fixture.probeCalls);
  }

  @Test
  @DisplayName("a failing notifier does not propagate, so the periodic task keeps running")
  void tick_notifierThrows_isSwallowed() {
    List<Integer> attempts = new ArrayList<>();
    MigrationKeepAlive keepAlive =
        new MigrationKeepAlive(
            new Fixture().executor,
            () -> 0L,
            () -> 0L,
            () -> "s",
            () -> false,
            (duration, text) -> {
              attempts.add(1);
              throw new IllegalArgumentException("boom");
            },
            WINDOW,
            STALL);
    keepAlive.start();

    keepAlive.tick();
    keepAlive.tick();

    assertEquals(2, attempts.size());
  }

  @Test
  @DisplayName("without a recorded backend pid the database check reports inactive")
  void isBackendActive_noPid_false() throws Exception {
    assertFalse(MigrationKeepAlive.isBackendActive("jdbc:postgresql://x/y", "u", "p", -1));
  }

  @Test
  @DisplayName("stop shuts the scheduler down")
  void stop_shutsDownScheduler() {
    Fixture fixture = new Fixture();

    fixture.keepAlive.start();
    fixture.keepAlive.stop();

    assertTrue(fixture.calls.contains("shutdownNow"));
  }

  @Test
  @DisplayName("the keep-alive thread is a daemon named WSC-BootKeepAlive")
  void newDaemonThread_isNamedDaemon() {
    Thread thread = MigrationKeepAlive.newDaemonThread(() -> {});

    assertEquals("WSC-BootKeepAlive", thread.getName());
    assertTrue(thread.isDaemon());
  }
}
