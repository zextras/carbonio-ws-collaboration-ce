// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
public class CacheVideoServerSession {
  private final Cache<Pair<UUID, String>, UUID> cache;
  private final ParticipantService participantService;
  private final ScheduledExecutorService scheduler;

  @Inject
  public CacheVideoServerSession(ParticipantService participantService) {
    this.participantService = participantService;
    this.scheduler =
        Executors.newScheduledThreadPool(
            1,
            r -> {
              Thread t = new Thread(r, "CacheVideoServerSession-Scheduler");
              t.setDaemon(true);
              return t;
            });
    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .removalListener(
                (Pair<UUID, String> key, UUID value, RemovalCause cause) -> {
                  if (cause == RemovalCause.EXPIRED && key != null) {
                    this.participantService.removeMeetingParticipant(value);
                  }
                })
            .executor(scheduler)
            .scheduler(Scheduler.systemScheduler())
            .build();

    // Schedule periodic cache cleanup to ensure timely removal listener invocation
    scheduler.scheduleAtFixedRate(cache::cleanUp, 0, 10, TimeUnit.SECONDS);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  scheduler.shutdown();
                  cache.invalidateAll();
                  try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                      scheduler.shutdownNow();
                    }
                  } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                  }
                },
                "CacheVideoServerSession-ShutdownHook"));
  }

  public void add(UUID userId, UUID queueId) {
    participantService
        .getByQueueId(queueId)
        .ifPresent(
            (Participant participant) -> {
              if (participant.getUserId().equals(userId.toString())) {
                cache.put(Pair.of(userId, participant.getMeeting().getId()), queueId);
              }
            });
  }

  public void remove(UUID userId, String meetingId) {
    cache.invalidate(Pair.of(userId, meetingId));
  }

  public UUID get(UUID userId, String meetingId) {
    return cache.getIfPresent(Pair.of(userId, meetingId));
  }

  public boolean contains(UUID userId, String meetingId) {
    return cache.getIfPresent(Pair.of(userId, meetingId)) != null;
  }
}
