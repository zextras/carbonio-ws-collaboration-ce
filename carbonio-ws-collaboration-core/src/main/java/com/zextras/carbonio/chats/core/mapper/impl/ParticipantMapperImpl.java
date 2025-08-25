// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.model.ParticipantDto;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class ParticipantMapperImpl implements ParticipantMapper {

  @Nullable
  @Override
  public ParticipantDto ent2dto(@Nullable Participant participant) {
    if (participant == null) {
      return null;
    }
    ParticipantDto participantDto =
        ParticipantDto.create()
            .userId(UUID.fromString(participant.getUserId()))
            .queueId(participant.getQueueId())
            .audioStreamEnabled(Optional.ofNullable(participant.hasAudioStreamOn()).orElse(false))
            .videoStreamEnabled(Optional.ofNullable(participant.hasVideoStreamOn()).orElse(false))
            .screenStreamEnabled(Optional.ofNullable(participant.hasScreenStreamOn()).orElse(false))
            .joinedAt(participant.getCreatedAt());
    Optional.ofNullable(participant.getHandRaisedAt()).ifPresent(participantDto::handRaisedAt);
    return participantDto;
  }

  @Override
  public List<ParticipantDto> ent2dto(@Nullable List<Participant> participants) {
    return Optional.ofNullable(participants).orElse(List.of()).stream().map(this::ent2dto).toList();
  }
}
