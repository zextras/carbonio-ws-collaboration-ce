// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MeetingDto(
    UUID id,
    String name,
    UUID roomId,
    String meetingType,
    boolean active,
    OffsetDateTime startedAt,
    OffsetDateTime createdAt,
    List<ParticipantDto> participants,
    @JsonIgnore Map<String, Object> extras) {

  /** Plugin-contributed fields, serialized at top level (e.g. recStartedAt). */
  @JsonAnyGetter
  public Map<String, Object> extraFields() {
    return extras;
  }
}
