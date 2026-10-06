// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import com.zextras.carbonio.ws.plugin.api.MeetingDtoEnricher;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One query per response, like ADV's batched MeetingMapperImpl.enrich(list). */
@ApplicationScoped
public class RecordingEnricher implements MeetingDtoEnricher {

  @Inject RecordingRepository recordings;

  @Override
  @Transactional
  public void enrich(List<MeetingView> meetings, Map<UUID, Map<String, Object>> extras) {
    recordings.findStarted(meetings.stream().map(m -> m.id().toString()).toList())
        .forEach(r -> {
          Map<String, Object> fields = extras.get(UUID.fromString(r.meetingId));
          fields.put("recStartedAt", r.startedAt);
          fields.put("recUserId", r.starterId);
        });
  }
}
