// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adds top-level JSON fields to meeting responses. Called once per response with every meeting in
 * it, so implementations can batch their queries. Put fields into {@code extras.get(meetingId)}.
 */
public interface MeetingDtoEnricher {
  void enrich(List<MeetingView> meetings, Map<UUID, Map<String, Object>> extras);
}
