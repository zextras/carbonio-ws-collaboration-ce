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
   * Performs all the actions necessary to add a 'participant' to a 'meeting' on the VideoServer
   *
   * @param userId        participant's user identifier
   * @param queueId       participant's message queue identifier
   * @param meetingId     id of the meeting
   * @param videoStreamOn true if participant wants to join with video, false otherwise
   * @param audioStreamOn true if participant wants to join with audio, false otherwise
   */
  void addMeetingParticipant(String userId, String queueId, String meetingId, boolean videoStreamOn,
    boolean audioStreamOn);

  /**
   * Performs all the actions necessary to destroy a 'participant' in a 'meeting' on the VideoServer
   *
   * @param userId    participant's user identifier
   * @param meetingId id of the meeting
   */
  void destroyMeetingParticipant(String userId, String meetingId);

  /**
   * Performs all the actions necessary to remove a 'participant' from a 'meeting' on the VideoServer
   *
   * @param userId    participant's user identifier
   * @param meetingId id of the meeting
   */
  void removeMeetingParticipant(String userId, String meetingId);

  /**
   * Updates the audio stream status for the user's session in the meeting
   *
   * @param userId    participant's user identifier
   * @param meetingId identification of the meeting on which to perform the operation
   * @param enabled   if true the audio stream is enabled, otherwise it is disabled
   */
  void updateAudioStream(String userId, String meetingId, boolean enabled);

  /**
   * Updates the media stream status for the user's session in the meeting
   *
   * @param userId                 participant's user identifier
   * @param meetingId              identification of the meeting on which to perform the operation
   * @param mediaStreamSettingsDto user settings request to update the media stream status
   */
  void updateMediaStream(String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto);

  /**
   * Completes WebRTC negotiation with VideoServer for the PeerConnection setup related to media stream.
   *
   * @param userId    participant's user identifier
   * @param meetingId identification of the meeting on which to perform the operation
   * @param sdp       the offer rtc session description
   */
  void answerRtcMediaStream(String userId, String meetingId, String sdp);

  /**
   * Update subscriptions of the current session to the desired media streams
   *
   * @param userId                 participant's user identifier
   * @param meetingId              identification of the meeting on which to perform the operation
   * @param subscriptionUpdatesDto contains all media streams which user wants to update subscriptions for
   */
  void updateSubscriptionsMediaStream(String userId, String meetingId,
    SubscriptionUpdatesDto subscriptionUpdatesDto);

  /**
   * Starts WebRTC negotiation with VideoServer for the PeerConnection setup related to audio stream.
   *
   * @param userId    participant's user identifier
   * @param meetingId identification of the meeting on which to perform the operation
   * @param sdp       the offer rtc session description
   */
  void offerRtcAudioStream(String userId, String meetingId, String sdp);
}
