// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.MockAppConfig;
import com.zextras.carbonio.chats.core.data.model.StaleParticipant;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@UnitTest
class StaleParticipantCleanerTest {

  private ScheduledExecutorService scheduler;
  private ParticipantRepository participantRepository;
  private ParticipantService participantService;
  private StaleParticipantCleaner cleaner;

  @BeforeEach
  void setUp() {
    scheduler = mock(ScheduledExecutorService.class);
    participantRepository = mock(ParticipantRepository.class);
    participantService = mock(ParticipantService.class);
    cleaner = cleanerWith(MockAppConfig.create());
  }

  private StaleParticipantCleaner cleanerWith(AppConfig appConfig) {
    return new StaleParticipantCleaner(
        participantRepository, participantService, appConfig, scheduler);
  }

  private static StaleParticipant staleParticipant() {
    return new StaleParticipant(
        UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
  }

  private void claimReturns(StaleParticipant... participants) {
    when(participantRepository.claimStale(Duration.ofHours(12))).thenReturn(List.of(participants));
  }

  @Test
  void runsOneMinuteAfterStartThenEveryTwelveHoursByDefault() {
    cleaner.start();

    verifySchedule(Duration.ofHours(12));
  }

  @Test
  void usesConfiguredThresholdAndIntervalIndependently() {
    cleaner =
        cleanerWith(
            MockAppConfig.create()
                .set(ConfigName.STALE_PARTICIPANT_THRESHOLD_IN_HOURS, "0.02")
                .set(ConfigName.STALE_PARTICIPANT_CLEANUP_INTERVAL_IN_HOURS, "0.005"));

    cleaner.start();
    cleaner.run();

    verifySchedule(Duration.ofSeconds(18));
    verify(participantRepository).claimStale(Duration.ofSeconds(72));
  }

  @Test
  void fallsBackToDefaultsForInvalidValues() {
    cleaner =
        cleanerWith(
            MockAppConfig.create()
                .set(ConfigName.STALE_PARTICIPANT_THRESHOLD_IN_HOURS, "invalid")
                .set(ConfigName.STALE_PARTICIPANT_CLEANUP_INTERVAL_IN_HOURS, "0"));

    cleaner.start();
    cleaner.run();

    verifySchedule(Duration.ofHours(12));
    verify(participantRepository).claimStale(Duration.ofHours(12));
  }

  private void verifySchedule(Duration interval) {
    verify(scheduler)
        .scheduleWithFixedDelay(
            any(Runnable.class),
            eq(Duration.ofMinutes(1).toSeconds()),
            eq(interval.toSeconds()),
            eq(TimeUnit.SECONDS));
  }

  @Test
  void removesEveryParticipantUntouchedForTwelveHours() {
    StaleParticipant first = staleParticipant();
    StaleParticipant second = staleParticipant();
    claimReturns(first, second);

    cleaner.run();

    verify(participantService).removeMeetingParticipant(UUID.fromString(first.queueId()));
    verify(participantService).removeMeetingParticipant(UUID.fromString(second.queueId()));
  }

  @Test
  void logsEachRemovedParticipant() {
    StaleParticipant participant = staleParticipant();
    claimReturns(participant);

    String expected =
        String.format(
            "Removed stale participant '%s' from meeting '%s'",
            participant.userId(), participant.meetingId());

    try (MockedStatic<ChatsLogger> logger = mockStatic(ChatsLogger.class)) {
      cleaner.run();

      logger.verify(() -> ChatsLogger.info(expected));
    }
  }

  @Test
  void keepsRemovingAndLogsWhenOneRemovalFails() {
    StaleParticipant failing = staleParticipant();
    StaleParticipant next = staleParticipant();
    claimReturns(failing, next);
    VideoServerException failure = new VideoServerException("unreachable");
    doThrow(failure)
        .when(participantService)
        .removeMeetingParticipant(UUID.fromString(failing.queueId()));

    String expected =
        String.format(
            "Unable to remove inactive participant '%s' from meeting '%s': %s",
            failing.userId(), failing.meetingId(), failure.getMessage());

    try (MockedStatic<ChatsLogger> logger = mockStatic(ChatsLogger.class)) {
      cleaner.run();

      logger.verify(() -> ChatsLogger.warn(expected));
    }
    verify(participantService).removeMeetingParticipant(UUID.fromString(next.queueId()));
  }

  @Test
  void aFailingClaimDoesNotCancelLaterRuns() {
    when(participantRepository.claimStale(any())).thenThrow(new IllegalStateException("db down"));

    assertDoesNotThrow(cleaner::run);
  }
}
