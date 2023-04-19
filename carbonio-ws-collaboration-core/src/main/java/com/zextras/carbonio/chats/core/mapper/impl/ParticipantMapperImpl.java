// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.meeting.model.ParticipantDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
public class ParticipantMapperImpl implements ParticipantMapper {

  @Nullable
  @Override
  public ParticipantDto ent2dto(@Nullable Participant participant) {
    if (participant == null) {
      return null;
    }
    return ParticipantDto.create()
      .userId(UUID.fromString(participant.getUserId()))
      .sessionId(participant.getSessionId())
      .audioStreamOn(Optional.ofNullable(participant.hasAudioStreamOn()).orElse(false))
      .videoStreamOn(Optional.ofNullable(participant.hasVideoStreamOn()).orElse(false))
      .screenStreamOn(Optional.ofNullable(participant.hasScreenStreamOn()).orElse(false));
  }

  @Override
  public List<ParticipantDto> ent2dto(@Nullable List<Participant> participants) {
    return Optional.ofNullable(participants).orElse(List.of()).stream().map(this::ent2dto).collect(Collectors.toList());
  }
}
