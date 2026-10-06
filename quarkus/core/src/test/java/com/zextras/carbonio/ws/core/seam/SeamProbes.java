// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import com.zextras.carbonio.ws.plugin.api.MeetingDtoEnricher;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStopping;
import com.zextras.carbonio.ws.testsupport.Seed;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;

/** Test plugin beans (@Singleton: client proxies would hide the public state). */
final class SeamProbes {

  private SeamProbes() {}

  /** Records MeetingStopping and whether the videoserver row still existed when it was fired. */
  @Singleton
  static class StoppingProbe {
    record Seen(MeetingStopping event, long videoServerRows) {}

    final List<Seen> seen = new CopyOnWriteArrayList<>();
    @Inject DataSource dataSource;

    void on(@Observes MeetingStopping event) {
      seen.add(
          new Seen(
              event,
              Seed.count(
                  dataSource,
                  "SELECT count(*) FROM chats.videoserver_meeting WHERE meeting_id = ?",
                  event.meetingId().toString())));
    }
  }

  @Singleton
  static class ProbeEnricher implements MeetingDtoEnricher {
    volatile boolean enabled;

    @Override
    public void enrich(List<MeetingView> meetings, Map<UUID, Map<String, Object>> extras) {
      if (enabled) {
        meetings.forEach(
            m -> extras.get(m.id()).put("probe", m.name()));
      }
    }
  }
}
