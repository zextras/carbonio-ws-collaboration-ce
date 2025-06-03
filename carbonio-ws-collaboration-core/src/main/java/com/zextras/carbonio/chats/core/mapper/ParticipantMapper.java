// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.model.ParticipantDto;
import jakarta.annotation.Nullable;
import java.util.List;

public interface ParticipantMapper {

  /**
   * Converts {@link Participant} to {@link ParticipantDto}
   *
   * @param participant {@link Participant} to convert
   * @return conversation result {@link ParticipantDto}
   */
  @Nullable
  ParticipantDto ent2dto(@Nullable Participant participant);

  /**
   * Converts a {@link List} of {@link Participant} to a {@link List} of {@link ParticipantDto}
   *
   * @param participants {@link List} of {@link Participant} to convert
   * @return conversation result ({@link List} of {@link ParticipantDto})
   */
  List<ParticipantDto> ent2dto(@Nullable List<Participant> participants);
}
