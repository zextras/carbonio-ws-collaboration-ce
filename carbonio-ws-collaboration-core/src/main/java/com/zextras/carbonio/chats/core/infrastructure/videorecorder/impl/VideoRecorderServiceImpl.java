// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Recording;
import com.zextras.carbonio.chats.core.data.model.RecordingInfo;
import com.zextras.carbonio.chats.core.data.type.RecordingStatus;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderClient;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderService;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request.VideoRecorderRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeCreateRequest;
import com.zextras.carbonio.chats.core.repository.RecordingRepository;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VideoRecorderServiceImpl implements VideoRecorderService {

  private final VideoRecorderClient videoRecorderClient;
  private final RecordingRepository recordingRepository;
  private final Clock clock;

  @Inject
  public VideoRecorderServiceImpl(
      VideoRecorderClient videoRecorderClient,
      RecordingRepository recordingRepository,
      Clock clock) {
    this.videoRecorderClient = videoRecorderClient;
    this.recordingRepository = recordingRepository;
    this.clock = clock;
  }

  @Override
  public void saveRecordingStarted(Meeting meeting, String userId, String token) {
    recordingRepository.insert(
        Recording.create()
            .id(UUID.randomUUID().toString())
            .meeting(meeting)
            .startedAt(OffsetDateTime.now(clock))
            .starterId(userId)
            .status(RecordingStatus.STARTED)
            .token(token));
  }

  @Override
  public void saveRecordingStopped(Recording recording) {
    recordingRepository.update(recording.status(RecordingStatus.STOPPED));
  }

  /**
   * This method allows you to send a request to the video recorder to start the post-processing
   * phase on the meeting recorded
   *
   * @param recordingInfo recording info needed by the video recorder
   * @see <a href="https://janus.conf.meetecho.com/docs/recordings.html">JanusRecordings</a>
   */
  @Override
  public CompletableFuture<Void> startRecordingPostProcessing(RecordingInfo recordingInfo) {
    return CompletableFuture.supplyAsync(
        () -> {
          videoRecorderClient.sendVideoRecorderRequest(
              recordingInfo.getServerId(),
              recordingInfo.getMeetingId(),
              Optional.ofNullable(recordingInfo.getRecordingToken())
                  .map(
                      token ->
                          VideoRecorderRequest.create()
                              .meetingId(recordingInfo.getMeetingId())
                              .meetingName(recordingInfo.getMeetingName())
                              .audioActivePackets(
                                  AudioBridgeCreateRequest.AUDIO_ACTIVE_PACKETS_DEFAULT)
                              .audioLevelAverage(
                                  AudioBridgeCreateRequest.AUDIO_LEVEL_AVERAGE_DEFAULT)
                              .authToken(AuthenticationMethod.ZM_AUTH_TOKEN + "=" + token)
                              .folderId(recordingInfo.getFolderId())
                              .recordingName(recordingInfo.getRecordingName()))
                  .orElse(
                      VideoRecorderRequest.create()
                          .meetingId(recordingInfo.getMeetingId())
                          .meetingName(recordingInfo.getMeetingName())
                          .audioActivePackets(AudioBridgeCreateRequest.AUDIO_ACTIVE_PACKETS_DEFAULT)
                          .audioLevelAverage(AudioBridgeCreateRequest.AUDIO_LEVEL_AVERAGE_DEFAULT)
                          .folderId(recordingInfo.getFolderId())
                          .recordingName(recordingInfo.getRecordingName())));
          return null;
        });
  }
}
