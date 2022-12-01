package com.zextras.carbonio.chats.meeting.infrastructure.videoserver;

public interface VideoServerService {

  void createMeeting(String meetingId);

  void deleteMeeting(String meetingId);

  void joinSession(String sessionId);
}
