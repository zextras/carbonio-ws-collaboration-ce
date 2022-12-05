package com.zextras.carbonio.chats.core.infrastructure.videoserver;

public interface JanusService {

  void createMeeting(String meetingId);

  void deleteMeeting(String meetingId);

  void joinMeeting(String userId, String sessionId, String meetingId, boolean webcamOn, boolean audioOn);

  void leaveMeeting(String userId, String sessionId, String meetingId);
}
