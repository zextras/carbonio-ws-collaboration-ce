// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zextras.carbonio.chats.model.MeetingDto;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A CE {@link MeetingDto} carrying the plugin enricher extras, serialised as top-level properties
 * next to the CE ones (e.g. {@code recStartedAt}). Only used when an enricher contributed
 * something, so the CE wire format of plain meetings is untouched.
 */
public class ExtensibleMeetingDto extends MeetingDto {

  @JsonIgnore private final Map<String, Object> extras = new LinkedHashMap<>();

  static ExtensibleMeetingDto of(MeetingDto dto, Map<String, Object> extras) {
    ExtensibleMeetingDto copy = new ExtensibleMeetingDto();
    copy.setId(dto.getId());
    copy.setName(dto.getName());
    copy.setRoomId(dto.getRoomId());
    copy.setActive(dto.isActive());
    copy.setParticipants(dto.getParticipants());
    copy.setCreatedAt(dto.getCreatedAt());
    copy.setStartedAt(dto.getStartedAt());
    copy.setMeetingType(dto.getMeetingType());
    copy.extras.putAll(extras);
    return copy;
  }

  @JsonAnyGetter
  public Map<String, Object> getExtras() {
    return extras;
  }
}
