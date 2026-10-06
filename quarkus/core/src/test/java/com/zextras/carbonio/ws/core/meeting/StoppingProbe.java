// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingRepository;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStopping;
import jakarta.inject.Singleton;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Singleton, not normal-scoped: tests read the public field, which a client proxy would not forward. Records each MeetingStopping and whether the videoserver row still existed when it fired. */
@Singleton
public class StoppingProbe {

  public record Seen(MeetingStopping event, boolean videoServerRowPresent) {}

  public final List<Seen> seen = new CopyOnWriteArrayList<>();

  @Inject VideoServerMeetingRepository videoServerMeetings;

  void onStopping(@Observes MeetingStopping event) {
    seen.add(new Seen(event, videoServerMeetings.findByIdOptional(event.meetingId().toString()).isPresent()));
  }
}
