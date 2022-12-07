package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;

public interface VideoServerService extends HealthIndicator {

  void createMeeting(String meetingId);

  void deleteMeeting(String meetingId);

  void joinMeeting(String userId, String sessionId, String meetingId, boolean webcamOn, boolean audioOn);

  void leaveMeeting(String userId, String sessionId, String meetingId);
}
