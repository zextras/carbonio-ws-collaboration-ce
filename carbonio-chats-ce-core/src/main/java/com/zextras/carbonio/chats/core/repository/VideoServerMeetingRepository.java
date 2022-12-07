package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import java.util.Optional;

public interface VideoServerMeetingRepository {

  Optional<VideoServerMeeting> getByMeetingId(String meetingId);

  VideoServerMeeting insert(VideoServerMeeting videoServerMeeting);

  void deleteById(String meetingId);
}
