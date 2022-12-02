package com.zextras.carbonio.chats.meeting.mapper;

import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
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
   * @return Conversation result {@link MeetingDto} {@link List}
   */
  @Nullable
  List<MeetingDto> ent2dto(@Nullable List<Meeting> meetings);
}
