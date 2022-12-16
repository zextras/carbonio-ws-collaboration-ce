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
  public void joinMeeting(String sessionId, String meetingId, boolean videoStreamOn, boolean audioStreamOn) {

  }

  @Override
  public void leaveMeeting(String sessionId, String meetingId) {

  }

  @Override
  public void enableVideoStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public void enableScreenShareStream(String sessionId, String meetingId, boolean enable) {

  }


  @Override
  public boolean isAlive() {
    return true;
  }
}
