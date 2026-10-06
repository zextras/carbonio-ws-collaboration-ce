// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

@ApplicationScoped
class JtaAfterCommit implements AfterCommit {

  @Inject TransactionSynchronizationRegistry registry;

  @Override
  public void run(Runnable task) {
    if (registry.getTransactionStatus() == Status.STATUS_NO_TRANSACTION) {
      task.run();
      return;
    }
    registry.registerInterposedSynchronization(new Synchronization() {
      @Override
      public void beforeCompletion() {}

      @Override
      public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
          task.run();
        }
      }
    });
  }
}
