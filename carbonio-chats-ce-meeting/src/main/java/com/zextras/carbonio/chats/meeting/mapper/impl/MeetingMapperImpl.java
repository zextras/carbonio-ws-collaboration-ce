package com.zextras.carbonio.chats.meeting.mapper.impl;

import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.mapper.MeetingMapper;
import com.zextras.carbonio.chats.meeting.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import java.util.UUID;
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
}
