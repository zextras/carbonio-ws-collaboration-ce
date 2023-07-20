// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;

public interface VideoServerService extends HealthIndicator {

  /**
   * Performs all the actions necessary to create a 'meeting' on the VideoServer
   *
   * @param meetingId meeting identifier
   */
  void startMeeting(String meetingId);

  /**
   * Performs all the actions necessary to delete a 'meeting' on the VideoServer
   *
   * @param meetingId meeting identifier
   */
  void stopMeeting(String meetingId);

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
   * Updates the audio stream status for the user's session in the meeting
   *
   * @param sessionId participant's session identifier
   * @param meetingId identification of the meeting on which to perform the operation
   * @param enabled   if true the audio stream is enabled, otherwise it is disabled
   */
  void updateAudioStream(String sessionId, String meetingId, boolean enabled);

  /**
   * Updates the media stream status for the user's session in the meeting
   *
   * @param sessionId              participant's session identifier
   * @param meetingId              identification of the meeting on which to perform the operation
   * @param mediaStreamSettingsDto user settings request to update the media stream status
   */
  void updateMediaStream(String sessionId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto);

  /**
   * Completes WebRTC negotiation with VideoServer for the PeerConnection setup related to media stream.
   *
   * @param sessionId participant's session identifier
   * @param meetingId identification of the meeting on which to perform the operation
   * @param sdp       the offer rtc session description
   */
  void answerRtcMediaStream(String sessionId, String meetingId, String sdp);

  /**
   * Update subscriptions of the current session to the desired media streams
   *
   * @param sessionId              participant's session identifier
   * @param meetingId              identification of the meeting on which to perform the operation
   * @param subscriptionUpdatesDto contains all media streams which user wants to update subscriptions for
   */
  void updateSubscriptionsMediaStream(String sessionId, String meetingId,
    SubscriptionUpdatesDto subscriptionUpdatesDto);

  /**
   * Starts WebRTC negotiation with VideoServer for the PeerConnection setup related to audio stream.
   *
   * @param sessionId participant's session identifier
   * @param meetingId identification of the meeting on which to perform the operation
   * @param sdp       the offer rtc session description
   */
  void offerRtcAudioStream(String sessionId, String meetingId, String sdp);
}
