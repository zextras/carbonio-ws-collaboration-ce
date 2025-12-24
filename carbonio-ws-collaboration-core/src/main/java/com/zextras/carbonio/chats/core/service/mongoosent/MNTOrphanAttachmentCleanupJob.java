// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled job that cleans up orphan attachments (uploaded but never linked to a message). Runs
 * every hour and deletes attachments that have been pending for more than 1 hour.
 */
@Singleton
public class MNTOrphanAttachmentCleanupJob {

  private static final int CLEANUP_INTERVAL_HOURS = 1;
  private static final int ORPHAN_AGE_HOURS = 1;

  private final MNTChatService chatService;
  private final ScheduledExecutorService scheduler;

  @Inject
  public MNTOrphanAttachmentCleanupJob(MNTChatService chatService) {
    this.chatService = chatService;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread thread = new Thread(r, "MNTOrphanAttachmentCleanup");
              thread.setDaemon(true);
              return thread;
            });
  }

  /** Starts the cleanup job. Should be called once at application startup. */
  public void start() {
    ChatsLogger.info("Starting MNT orphan attachment cleanup job (interval: 1 hour)");
    scheduler.scheduleAtFixedRate(
        this::runCleanup, CLEANUP_INTERVAL_HOURS, CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS);
  }

  /** Stops the cleanup job. Should be called at application shutdown. */
  public void stop() {
    ChatsLogger.info("Stopping MNT orphan attachment cleanup job");
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void runCleanup() {
    try {
      int cleaned = chatService.cleanupOrphanAttachments(ORPHAN_AGE_HOURS);
      if (cleaned > 0) {
        ChatsLogger.info("MNT orphan attachment cleanup: deleted " + cleaned + " orphan attachments");
      }
    } catch (Exception e) {
      ChatsLogger.warn("Error during MNT orphan attachment cleanup: " + e.getMessage());
    }
  }
}
