package com.zextras.carbonio.chats.core.infrastructure.videoserver;

public interface VideoServerService {

  void createMeeting(String meetingId);

  void deleteMeeting(String meetingId);

  void joinSession(String sessionId);

  void leaveSession(String sessionId);
}