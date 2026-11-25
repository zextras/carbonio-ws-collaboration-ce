// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository.StaleParticipant;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class StaleParticipantCleaner {

  private static final Duration FIRST_RUN_DELAY = Duration.ofMinutes(1);
  private static final Duration RUN_INTERVAL = Duration.ofHours(12);
  private static final Duration STALE_AFTER = Duration.ofHours(12);

  private final ParticipantRepository participantRepository;
  private final ParticipantService participantService;
  private final ScheduledExecutorService scheduler;

  @Inject
  public StaleParticipantCleaner(
      ParticipantRepository participantRepository, ParticipantService participantService) {
    this(
        participantRepository,
        participantService,
        Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("StaleParticipantCleaner").daemon().factory()));
  }

  StaleParticipantCleaner(
      ParticipantRepository participantRepository,
      ParticipantService participantService,
      ScheduledExecutorService scheduler) {
    this.participantRepository = participantRepository;
    this.participantService = participantService;
    this.scheduler = scheduler;
  }

  public void start() {
    scheduler.scheduleWithFixedDelay(
        this::run, FIRST_RUN_DELAY.toMillis(), RUN_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
  }

  // An escaping throwable would cancel every later run.
  void run() {
    try {
      participantRepository.claimStale(STALE_AFTER).forEach(this::remove);
    } catch (Throwable t) {
      ChatsLogger.error("Stale meeting participant cleanup failed", t);
    }
  }

  private void remove(StaleParticipant participant) {
    try {
      participantService.removeMeetingParticipant(UUID.fromString(participant.queueId()));
      ChatsLogger.info(
          String.format(
              "Removed participant '%s' from meeting '%s': inactive for %d hours",
              participant.userId(), participant.meetingId(), STALE_AFTER.toHours()));
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format(
              "Unable to remove inactive participant '%s' from meeting '%s', retrying in %d"
                  + " hours: %s",
              participant.userId(),
              participant.meetingId(),
              RUN_INTERVAL.toHours(),
              e.getMessage()));
    }
  }
}
