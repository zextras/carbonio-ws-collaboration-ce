// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import com.zextras.carbonio.ws.core.meeting.dto.MeetingDto;
import com.zextras.carbonio.ws.core.meeting.dto.ParticipantDto;
import com.zextras.carbonio.ws.core.persistence.MeetingEntity;
import com.zextras.carbonio.ws.core.persistence.ParticipantEntity;
import com.zextras.carbonio.ws.core.persistence.ParticipantRepository;
import com.zextras.carbonio.ws.plugin.api.MeetingDtoEnricher;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MeetingMapper {

  @Inject ParticipantRepository participants;
  @Inject Instance<MeetingDtoEnricher> enrichers;

  public MeetingView view(MeetingEntity meeting) {
    return view(meeting, participants.listByMeeting(meeting.id));
  }

  public MeetingDto toDto(MeetingEntity meeting) {
    return toDtos(List.of(meeting)).get(0);
  }

  public List<MeetingDto> toDtos(List<MeetingEntity> meetings) {
    Map<String, List<ParticipantEntity>> byMeeting = new HashMap<>();
    meetings.forEach(m -> byMeeting.put(m.id, participants.listByMeeting(m.id)));
    List<MeetingView> views = meetings.stream().map(m -> view(m, byMeeting.get(m.id))).toList();
    Map<UUID, Map<String, Object>> extras = new HashMap<>();
    views.forEach(v -> extras.put(v.id(), new LinkedHashMap<>()));
    for (MeetingDtoEnricher enricher : enrichers) {
      enricher.enrich(views, extras);
    }
    List<MeetingDto> result = new ArrayList<>();
    for (MeetingEntity m : meetings) {
      result.add(
          new MeetingDto(
              UUID.fromString(m.id),
              m.name,
              UUID.fromString(m.roomId),
              m.meetingType.toLowerCase(),
              Boolean.TRUE.equals(m.active),
              m.startedAt,
              m.createdAt,
              byMeeting.get(m.id).stream().map(MeetingMapper::participant).toList(),
              extras.get(UUID.fromString(m.id))));
    }
    return result;
  }

  private static MeetingView view(MeetingEntity m, List<ParticipantEntity> ps) {
    return new MeetingView(
        UUID.fromString(m.id),
        m.name,
        UUID.fromString(m.roomId),
        Boolean.TRUE.equals(m.active),
        ps.stream().map(p -> UUID.fromString(p.userId)).toList());
  }

  private static ParticipantDto participant(ParticipantEntity p) {
    return new ParticipantDto(
        UUID.fromString(p.userId),
        Optional.ofNullable(p.queueId).map(UUID::fromString).orElse(null),
        Boolean.TRUE.equals(p.audioStreamOn),
        Boolean.TRUE.equals(p.videoStreamOn),
        Boolean.TRUE.equals(p.screenStreamOn),
        p.createdAt);
  }
}
