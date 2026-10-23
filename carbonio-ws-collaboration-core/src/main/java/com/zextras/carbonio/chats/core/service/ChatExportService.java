// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only
package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.StreamingOutput;
import java.time.ZoneId;
import java.util.UUID;

public interface ChatExportService {

  /**
   * Returns a streaming body that writes the full message history of {@code roomId} (oldest→newest)
   * as plain text, honoring the user's cleared-history lower bound. The DB is read in keyset
   * batches so memory stays constant regardless of room size.
   */
  StreamingOutput export(
      UUID roomId, UserPrincipal principal, ZoneId zone, String deletedPlaceholder);
}
