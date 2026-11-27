// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository.StaleParticipant;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class StaleParticipantCleaner {

  private static final Duration FIRST_RUN_DELAY = Duration.ofMinutes(1);
  private static final Duration DEFAULT_DURATION = Duration.ofHours(12);

  private final ParticipantRepository participantRepository;
  private final ParticipantService participantService;
  private final ScheduledExecutorService scheduler;
  private final Duration staleAfter;
  private final Duration runInterval;

  @Inject
  public StaleParticipantCleaner(
      ParticipantRepository participantRepository,
      ParticipantService participantService,
      AppConfig appConfig) {
    this(
        participantRepository,
        participantService,
        appConfig,
        Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("StaleParticipantCleaner").daemon().factory()));
  }

  StaleParticipantCleaner(
      ParticipantRepository participantRepository,
      ParticipantService participantService,
      AppConfig appConfig,
      ScheduledExecutorService scheduler) {
    this.participantRepository = participantRepository;
    this.participantService = participantService;
    this.scheduler = scheduler;
    staleAfter = configuredDuration(appConfig, ConfigName.STALE_PARTICIPANT_THRESHOLD_IN_HOURS);
    runInterval =
        configuredDuration(appConfig, ConfigName.STALE_PARTICIPANT_CLEANUP_INTERVAL_IN_HOURS);
  }

  private static Duration configuredDuration(AppConfig appConfig, ConfigName key) {
    try {
      return appConfig
          .get(String.class, key)
          .map(StaleParticipantCleaner::parseHours)
          .orElse(DEFAULT_DURATION);
    } catch (IllegalArgumentException | ArithmeticException e) {
      ChatsLogger.warn(
          "Invalid hours for " + key + "; using " + DEFAULT_DURATION.toHours() + " hours");
      return DEFAULT_DURATION;
    }
  }

  private static Duration parseHours(String value) {
    long seconds = new BigDecimal(value.trim()).multiply(BigDecimal.valueOf(3600)).longValueExact();
    if (seconds <= 0) {
      throw new IllegalArgumentException("Expected positive hours");
    }
    return Duration.ofSeconds(seconds);
  }

  public void start() {
    scheduler.scheduleWithFixedDelay(
        this::run, FIRST_RUN_DELAY.toSeconds(), runInterval.toSeconds(), TimeUnit.SECONDS);
    ChatsLogger.info(
        String.format(
            "Stale participant cleanup scheduled: threshold %ds, interval %ds",
            staleAfter.toSeconds(), runInterval.toSeconds()));
  }

  // An escaping exception would cancel every later run.
  void run() {
    try {
      participantRepository.claimStale(staleAfter).forEach(this::remove);
    } catch (Exception e) {
      ChatsLogger.error("Stale meeting participant cleanup failed", e);
    }
  }

  private void remove(StaleParticipant participant) {
    try {
      participantService.removeMeetingParticipant(UUID.fromString(participant.queueId()));
      ChatsLogger.info(
          String.format(
              "Removed stale participant '%s' from meeting '%s'",
              participant.userId(), participant.meetingId()));
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format(
              "Unable to remove inactive participant '%s' from meeting '%s': %s",
              participant.userId(), participant.meetingId(), e.getMessage()));
    }
  }
}
