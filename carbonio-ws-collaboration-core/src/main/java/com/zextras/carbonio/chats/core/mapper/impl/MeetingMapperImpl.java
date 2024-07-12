// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.type.RecordingStatus;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
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
    MeetingDto meetingDto =
        MeetingDto.create()
            .id(UUID.fromString(meeting.getId()))
            .roomId(UUID.fromString(meeting.getRoomId()))
            .meetingType(MeetingTypeDto.fromString(meeting.getMeetingType().toString()))
            .name(meeting.getName())
            .createdAt(meeting.getCreatedAt())
            .startedAt(meeting.getStartedAt())
            .active(meeting.getActive())
            .participants(participantMapper.ent2dto(meeting.getParticipants()));
    meeting.getRecordings().stream()
        .filter(r -> RecordingStatus.STARTED.equals(r.getStatus()))
        .findFirst()
        .ifPresent(
            value ->
                meetingDto
                    .recStartedAt(value.getStartedAt())
                    .recUserId(UUID.fromString(value.getStarterId())));
    return meetingDto;
  }

  @Override
  @Nullable
  public List<MeetingDto> ent2dto(@Nullable List<Meeting> meetings) {
    if (meetings == null) {
      return List.of();
    }
    return meetings.stream().map(this::ent2dto).toList();
  }
}
