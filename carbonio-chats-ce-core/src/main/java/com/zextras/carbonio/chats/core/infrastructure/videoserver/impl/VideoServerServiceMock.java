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
  public void joinMeeting(String userId, String sessionId, String meetingId, boolean webcamOn, boolean audioOn) {

  }

  @Override
  public void leaveMeeting(String userId, String sessionId, String meetingId) {

  }


  @Override
  public boolean isAlive() {
    return true;
  }
}
