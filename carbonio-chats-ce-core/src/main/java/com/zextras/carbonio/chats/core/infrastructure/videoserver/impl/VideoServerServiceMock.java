package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import javax.inject.Singleton;

@Singleton
public class VideoServerServiceMock implements VideoServerService {

  @Override
  public void createMeeting(String meetingId) {

  }

  @Override
  public void deleteMeeting(String meetingId) {

  }

  @Override
  public void joinSession(String sessionId) {

  }

  @Override
  public void leaveSession(String sessionId) {

  }
}
