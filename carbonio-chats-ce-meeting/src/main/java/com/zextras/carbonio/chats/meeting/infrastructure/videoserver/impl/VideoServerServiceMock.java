package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.impl;

import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import javax.inject.Singleton;

@Singleton
public class VideoServerServiceMock implements VideoServerService {

  @Override
  public void createMeeting(String meetingId) {
  }
}