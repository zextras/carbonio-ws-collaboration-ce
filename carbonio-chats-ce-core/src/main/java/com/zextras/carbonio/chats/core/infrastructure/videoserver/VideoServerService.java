package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;

public interface VideoServerService extends HealthIndicator {

  /**
   * Performs all the actions necessary to create a 'meeting' on the VideoServer
   *
   * @param meetingId meeting identifier
   */
  void createMeeting(String meetingId);

  /**
   * Performs all the actions necessary to delete a 'meeting' on the VideoServer
   *
   * @param meetingId meeting identifier
   */
  void deleteMeeting(String meetingId);

  /**
   * Performs all the actions necessary to join a 'participant' in a 'meeting' on the VideoServer
   *
   * @param sessionId     participant's session identifier
   * @param meetingId     id of the meeting to join
   * @param videoStreamOn true if participant wants to join with video, false otherwise
   * @param audioStreamOn true if participant wants to join with audio, false otherwise
   */
  void joinMeeting(String sessionId, String meetingId, boolean videoStreamOn, boolean audioStreamOn);

  /**
   * Performs all the actions necessary to leave a 'participant' from a 'meeting' on the VideoServer
   *
   * @param sessionId participant's session identifier
   * @param meetingId id of the meeting to leave
   */
  void leaveMeeting(String sessionId, String meetingId);

  /**
   * Enable or disable the video stream for the user's session in the meeting
   *
   * @param sessionId participant's session identifier
   * @param meetingId identification of the meeting on which to perform the operation
   * @param enable    if true the video stream will be enabled, otherwise it will be disabled
   */
  void enableVideoStream(String sessionId, String meetingId, boolean enable);
}
