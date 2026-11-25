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
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository.StaleParticipant;
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
    cleaner = new StaleParticipantCleaner(participantRepository, participantService, scheduler);
  }

  private static StaleParticipant staleParticipant() {
    return new StaleParticipant(
        UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
  }

  private void claimReturns(StaleParticipant... participants) {
    when(participantRepository.claimStale(Duration.ofHours(12))).thenReturn(List.of(participants));
  }

  @Test
  void runsOneMinuteAfterStartThenEveryTwelveHours() {
    cleaner.start();

    verify(scheduler)
        .scheduleWithFixedDelay(
            any(Runnable.class),
            eq(Duration.ofMinutes(1).toMillis()),
            eq(Duration.ofHours(12).toMillis()),
            eq(TimeUnit.MILLISECONDS));
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
            "Removed participant '%s' from meeting '%s': inactive for 12 hours",
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
            "Unable to remove inactive participant '%s' from meeting '%s', retrying in 12 hours:"
                + " %s",
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
