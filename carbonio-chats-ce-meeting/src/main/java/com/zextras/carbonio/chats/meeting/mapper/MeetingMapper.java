package com.zextras.carbonio.chats.meeting.mapper;

import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import javax.annotation.Nullable;

public interface MeetingMapper {

  /**
   * Convert a {@link Meeting} to {@link MeetingDto}
   *
   * @param meeting {@link Meeting} to convert
   * @return Conversation result {@link MeetingDto}
   */
  @Nullable
  MeetingDto ent2dto(@Nullable Meeting meeting);
}
