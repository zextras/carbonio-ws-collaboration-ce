// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.model.RecordingInfo;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import java.util.List;

public interface VideoServerService extends HealthIndicator {

  void startMeeting(String meetingId);

  void stopMeeting(String meetingId);

  void addMeetingParticipant(
      String userId,
      String queueId,
      String meetingId,
      boolean videoStreamOn,
      boolean audioStreamOn);

  void destroyMeetingParticipant(String userId, String meetingId);

  List<VideoServerSession> getSessions(String meetingId);

  void updateAudioStream(String userId, String meetingId, boolean enabled);

  void updateMediaStream(
      String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto);

  void answerRtcMediaStream(String userId, String meetingId, String sdp);

  void updateSubscriptionsMediaStream(
      String userId, String meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto);

  void offerRtcAudioStream(String userId, String meetingId, String sdp);

  void startRecording(String meetingId);

  void stopRecording(String meetingId, RecordingInfo recordingInfo);
}
