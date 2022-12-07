package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.VideoServerSessionUser;
import java.util.List;

public interface VideoServerSessionUserRepository {

  List<VideoServerSessionUser> getByMeetingId(String meetingId);

  VideoServerSessionUser insert(VideoServerSessionUser videoServerSessionUser);

  boolean remove(VideoServerSessionUser videoServerSessionUser);
}
