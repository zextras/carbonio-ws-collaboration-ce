// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deletes expired send claims every {@value INTERVAL_MINUTES} minutes. Every instance runs it; the
 * batches skip rows locked by another instance.
 */
@Singleton
public class MessageSendDedupPurgeService {

  static final long INTERVAL_MINUTES = 10;
  static final int BATCH_SIZE = 10_000;

  private final MessageSendDedupRepository repository;
  private final ScheduledExecutorService scheduler;

  @Inject
  public MessageSendDedupPurgeService(MessageSendDedupRepository repository) {
    this(
        repository,
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "WSC-SendDedupPurge");
              t.setDaemon(true);
              return t;
            }));
  }

  MessageSendDedupPurgeService(
      MessageSendDedupRepository repository, ScheduledExecutorService scheduler) {
    this.repository = repository;
    this.scheduler = scheduler;
  }

  /** Starts the periodic purge. Called explicitly from {@code Boot} after the server starts. */
  public void start() {
    scheduler.scheduleWithFixedDelay(
        this::purge, INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);
    ChatsLogger.info(
        "MessageSendDedupPurgeService started, running every %d minutes"
            .formatted(INTERVAL_MINUTES));
  }

  void purge() {
    try {
      int total = 0;
      int deleted;
      do {
        deleted = repository.purgeExpired(BATCH_SIZE);
        total += deleted;
      } while (deleted == BATCH_SIZE);
      if (total > 0) {
        ChatsLogger.debug("MessageSendDedupPurge: deleted %d expired claims".formatted(total));
      }
    } catch (Exception e) {
      ChatsLogger.warn("MessageSendDedupPurge: error during purge: " + e.getMessage());
    }
  }
}
