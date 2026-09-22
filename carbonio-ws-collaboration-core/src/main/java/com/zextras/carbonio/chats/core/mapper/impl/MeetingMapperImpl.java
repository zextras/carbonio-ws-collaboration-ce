// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Singleton
public class MeetingMapperImpl implements MeetingMapper {

  private final ParticipantMapper participantMapper;

  @Inject
  public MeetingMapperImpl(ParticipantMapper participantMapper) {
    this.participantMapper = participantMapper;
  }

  @Override
  @Nullable
  public MeetingDto ent2dto(@Nullable Meeting meeting) {
    if (meeting == null) {
      return null;
    }
    MeetingDto dto = toDto(meeting);
    enrich(dto, meeting);
    return dto;
  }

  private MeetingDto toDto(Meeting meeting) {
    return MeetingDto.create()
        .id(UUID.fromString(meeting.getId()))
        .roomId(UUID.fromString(meeting.getRoomId()))
        .meetingType(MeetingTypeDto.fromString(meeting.getMeetingType().toString()))
        .name(meeting.getName())
        .createdAt(meeting.getCreatedAt())
        .startedAt(meeting.getStartedAt())
        .active(meeting.getActive())
        .participants(participantMapper.ent2dto(meeting.getParticipants()));
  }

  protected void enrich(MeetingDto dto, Meeting meeting) {}

  protected void enrich(List<MeetingDto> dtos, List<Meeting> meetings) {
    for (int i = 0; i < meetings.size(); i++) {
      enrich(dtos.get(i), meetings.get(i));
    }
  }

  @Override
  @Nullable
  public List<MeetingDto> ent2dto(@Nullable List<Meeting> meetings) {
    if (meetings == null) {
      return List.of();
    }
    List<MeetingDto> dtos = meetings.stream().map(this::toDto).toList();
    enrich(dtos, meetings);
    return dtos;
  }
}
