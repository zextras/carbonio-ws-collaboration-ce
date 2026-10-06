// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

/** Runs a task only once the current transaction has committed; dropped on rollback. */
interface AfterCommit {
  void run(Runnable task);
}
