// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.meeting.model.MeetingDto;
import java.util.List;
import javax.annotation.Nullable;

public interface MeetingMapper {

  /**
   * Converts a {@link Meeting} to {@link MeetingDto}
   *
   * @param meeting {@link Meeting} to convert
   * @return Conversation result {@link MeetingDto}
   */
  @Nullable
  MeetingDto ent2dto(@Nullable Meeting meeting);

  /**
   * Converts a {@link Meeting} {@link List} into a {@link MeetingDto} {@link List}
   *
   * @param meetings {@link Meeting} {@link List} to convert
   * @return Conversation result ({@link List} of {@link MeetingDto})
   */
  @Nullable
  List<MeetingDto> ent2dto(@Nullable List<Meeting> meetings);
}
