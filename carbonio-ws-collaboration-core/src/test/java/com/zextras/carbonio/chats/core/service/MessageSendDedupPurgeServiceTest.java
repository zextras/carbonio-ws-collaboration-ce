// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MessageSendDedupPurgeServiceTest {

  private static final int BATCH = MessageSendDedupPurgeService.BATCH_SIZE;

  private final MessageSendDedupRepository repository = mock(MessageSendDedupRepository.class);
  private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
  private final MessageSendDedupPurgeService service =
      new MessageSendDedupPurgeService(repository, scheduler);

  @Test
  @DisplayName("start schedules the purge every 10 minutes with a fixed delay")
  void start_schedulesEveryTenMinutes() {
    service.start();

    verify(scheduler)
        .scheduleWithFixedDelay(any(Runnable.class), eq(10L), eq(10L), eq(TimeUnit.MINUTES));
  }

  @Test
  @DisplayName("The scheduled task is the purge")
  void start_scheduledTaskPurges() {
    when(repository.purgeExpired(anyInt())).thenReturn(0);
    service.start();
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).scheduleWithFixedDelay(task.capture(), anyLong(), anyLong(), any());

    task.getValue().run();

    verify(repository).purgeExpired(BATCH);
  }

  @Test
  @DisplayName("purge keeps deleting while whole batches are deleted")
  void purge_repeatsFullBatches() {
    when(repository.purgeExpired(BATCH)).thenReturn(BATCH, BATCH, 3);

    service.purge();

    verify(repository, times(3)).purgeExpired(BATCH);
  }

  @Test
  @DisplayName("purge stops after an empty batch")
  void purge_stopsOnEmptyBatch() {
    when(repository.purgeExpired(anyInt())).thenReturn(0);

    service.purge();

    verify(repository, times(1)).purgeExpired(BATCH);
  }

  @Test
  @DisplayName("purge swallows a repository failure so that the next run still happens")
  void purge_failure_doesNotThrow() {
    when(repository.purgeExpired(anyInt())).thenThrow(new RuntimeException("db down"));

    assertDoesNotThrow(service::purge);
  }
}
