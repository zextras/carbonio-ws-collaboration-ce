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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface VideoServerService extends HealthIndicator {

  CompletableFuture<Void> startMeeting(String meetingId);

  CompletableFuture<Void> stopMeeting(String meetingId);

  CompletableFuture<Void> addMeetingParticipant(
      String userId,
      String queueId,
      String meetingId,
      boolean videoStreamOn,
      boolean audioStreamOn);

  CompletableFuture<Void> destroyMeetingParticipant(String userId, String meetingId);

  Optional<VideoServerSession> getSession(String connectionId);

  List<VideoServerSession> getSessions(String meetingId);

  CompletableFuture<Void> updateAudioStream(String userId, String meetingId, boolean enabled);

  CompletableFuture<Void> updateMediaStream(
      String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto);

  CompletableFuture<Void> answerRtcMediaStream(String userId, String meetingId, String sdp);

  CompletableFuture<Void> updateSubscriptionsMediaStream(
      String userId, String meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto);

  CompletableFuture<Void> offerRtcAudioStream(String userId, String meetingId, String sdp);

  CompletableFuture<Void> startRecording(String meetingId);

  CompletableFuture<Void> stopRecording(String meetingId, RecordingInfo recordingInfo);
}
