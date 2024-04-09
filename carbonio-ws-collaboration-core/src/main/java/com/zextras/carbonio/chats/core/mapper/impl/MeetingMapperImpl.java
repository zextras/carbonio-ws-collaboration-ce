// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    return MeetingDto.create()
        .id(UUID.fromString(meeting.getId()))
        .roomId(UUID.fromString(meeting.getRoomId()))
        .meetingType(MeetingTypeDto.fromString(meeting.getMeetingType().toString()))
        .name(meeting.getName())
        .createdAt(meeting.getCreatedAt())
        .active(meeting.getActive())
        .participants(participantMapper.ent2dto(meeting.getParticipants()));
  }

  @Override
  @Nullable
  public List<MeetingDto> ent2dto(@Nullable List<Meeting> meetings) {
    if (meetings == null) {
      return List.of();
    }
    return meetings.stream().map(this::ent2dto).collect(Collectors.toList());
  }
}
