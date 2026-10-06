// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import com.zextras.carbonio.ws.plugin.api.MeetingDtoEnricher;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Adds "probe" only to meetings named "enrich-me", so other tests see the plain DTO. */
@ApplicationScoped
public class ProbeEnricher implements MeetingDtoEnricher {
  @Override
  public void enrich(List<MeetingView> meetings, Map<UUID, Map<String, Object>> extras) {
    meetings.stream()
        .filter(m -> "enrich-me".equals(m.name()))
        .forEach(m -> extras.get(m.id()).put("probe", "enriched-" + meetings.size()));
  }
}
