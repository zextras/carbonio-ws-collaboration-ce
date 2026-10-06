// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.ws.plugin.api.MeetingDtoEnricher;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The CE meeting mapper with ADV's {@code enrich} hook turned into the plugin {@link
 * MeetingDtoEnricher}s: each enricher sees all the mapped meetings at once, and meetings that got
 * extras are returned as {@link ExtensibleMeetingDto} (extras top-level in the JSON).
 */
@Singleton // the CE superclass has no no-args constructor, so no client proxy
@Alternative
@Priority(1)
public class PluginMeetingMapper extends MeetingMapperImpl {

  private final Instance<MeetingDtoEnricher> enrichers;

  @Inject
  public PluginMeetingMapper(
      ParticipantMapper participantMapper, Instance<MeetingDtoEnricher> enrichers) {
    super(participantMapper);
    this.enrichers = enrichers;
  }

  @Override
  public MeetingDto ent2dto(Meeting meeting) {
    MeetingDto dto = super.ent2dto(meeting);
    return dto == null ? null : withExtras(List.of(dto), List.of(meeting)).get(0);
  }

  @Override
  public List<MeetingDto> ent2dto(List<Meeting> meetings) {
    List<MeetingDto> dtos = super.ent2dto(meetings);
    return meetings == null ? dtos : withExtras(dtos, meetings);
  }

  private List<MeetingDto> withExtras(List<MeetingDto> dtos, List<Meeting> meetings) {
    if (enrichers.isUnsatisfied() || dtos.isEmpty()) {
      return dtos;
    }
    Map<UUID, Map<String, Object>> extras = new HashMap<>();
    var views = meetings.stream().map(MeetingViews::of).toList();
    for (MeetingDtoEnricher enricher : enrichers) {
      enricher.enrich(views, extras);
    }
    if (extras.isEmpty()) {
      return dtos;
    }
    List<MeetingDto> result = new ArrayList<>(dtos.size());
    for (MeetingDto dto : dtos) {
      Map<String, Object> own = extras.get(dto.getId());
      result.add(own == null || own.isEmpty() ? dto : ExtensibleMeetingDto.of(dto, own));
    }
    return List.copyOf(result);
  }
}
