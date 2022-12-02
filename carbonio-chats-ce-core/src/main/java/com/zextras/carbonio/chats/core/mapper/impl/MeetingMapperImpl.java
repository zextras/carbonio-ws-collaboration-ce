package com.zextras.carbonio.chats.core.mapper.impl;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.meeting.model.MeetingDto;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

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
      .createdAt(meeting.getCreatedAt())
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
