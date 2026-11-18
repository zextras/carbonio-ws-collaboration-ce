// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.presence;

import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import java.util.Collection;
import java.util.List;

public interface PresenceService {

  /**
   * Returns presence summaries for a batch of user identifiers. Users not found are returned as
   * offline with a {@code null} last-seen timestamp.
   *
   * @param userIds collection of user identifiers to query
   * @return list of {@link PresenceSummary}
   */
  List<PresenceSummary> getBatchPresence(Collection<String> userIds);

  /**
   * Gets users who should receive presence updates for a user
   *
   * @param userId user identifier
   * @return list of user IDs to notify
   */
  List<String> getPresenceSubscribers(String userId);
}
