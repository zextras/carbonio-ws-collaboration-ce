// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Maps CE meetings to the plugin-facing {@link MeetingView}. */
final class MeetingViews {

  private MeetingViews() {}

  static MeetingView of(Meeting meeting) {
    List<UUID> participants =
        Optional.ofNullable(meeting.getParticipants()).orElse(List.of()).stream()
            .map(Participant::getUserId)
            .distinct()
            .map(UUID::fromString)
            .toList();
    return new MeetingView(
        UUID.fromString(meeting.getId()),
        meeting.getName(),
        UUID.fromString(meeting.getRoomId()),
        Boolean.TRUE.equals(meeting.getActive()),
        participants);
  }
}
