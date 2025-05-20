// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Recording;
import com.zextras.carbonio.chats.core.data.model.RecordingInfo;
import com.zextras.carbonio.chats.core.data.type.RecordingStatus;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderClient;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderService;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request.VideoRecorderRequest;
import com.zextras.carbonio.chats.core.repository.RecordingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VideoRecorderServiceImplTest {

  private final VideoRecorderClient videoRecorderClient;
  private final RecordingRepository recordingRepository;
  private final VideoRecorderService videoRecorderService;

  public VideoRecorderServiceImplTest() {
    this.videoRecorderClient = mock(VideoRecorderClient.class);
    this.recordingRepository = mock(RecordingRepository.class);
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2022-01-01T11:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneId.of("UTC+00:00"));

    this.videoRecorderService =
        new VideoRecorderServiceImpl(videoRecorderClient, recordingRepository, clock);
  }

  private UUID meeting1Id;
  private UUID recordingId;
  private UUID userId;

  @BeforeEach
  void init() {
    meeting1Id = UUID.randomUUID();
    recordingId = UUID.randomUUID();
    userId = UUID.randomUUID();
  }

  @AfterEach
  void cleanup() {
    verifyNoMoreInteractions(videoRecorderClient, recordingRepository);
    reset(videoRecorderClient, recordingRepository);
  }

  @Nested
  @DisplayName("Save recording started tests")
  class SaveRecordingStartedTests {

    @Test
    @DisplayName("Save recording as started in the repository")
    void saveRecordingStarted_testOk() {
      Meeting meeting = Meeting.create().id(meeting1Id.toString());
      videoRecorderService.saveRecordingStarted(meeting, userId.toString(), "fake-token");

      ArgumentCaptor<Recording> recordingArgumentCaptor = ArgumentCaptor.forClass(Recording.class);
      verify(recordingRepository, times(1)).insert(recordingArgumentCaptor.capture());

      assertEquals(1, recordingArgumentCaptor.getAllValues().size());
      Recording recording = recordingArgumentCaptor.getValue();
      assertFalse(recording.getId().isEmpty());
      assertEquals(meeting, recording.getMeeting());
      assertEquals(OffsetDateTime.parse("2022-01-01T11:00:00Z"), recording.getStartedAt());
      assertEquals(userId.toString(), recording.getStarterId());
      assertEquals(RecordingStatus.STARTED, recording.getStatus());
    }
  }

  @Nested
  @DisplayName("Save recording stopped tests")
  class SaveRecordingStoppedTests {

    @Test
    @DisplayName("Save recording as stopped in the repository")
    void saveRecordingStopped_testOk() {
      Meeting meeting = Meeting.create().id(meeting1Id.toString());
      Recording recording =
          Recording.create()
              .id(recordingId.toString())
              .meeting(meeting)
              .startedAt(OffsetDateTime.parse("2022-01-01T11:00:00Z"))
              .starterId(userId.toString())
              .status(RecordingStatus.STARTED);
      videoRecorderService.saveRecordingStopped(recording);

      ArgumentCaptor<Recording> recordingArgumentCaptor = ArgumentCaptor.forClass(Recording.class);
      verify(recordingRepository, times(1)).update(recordingArgumentCaptor.capture());

      assertEquals(1, recordingArgumentCaptor.getAllValues().size());
      Recording recordingUpdated = recordingArgumentCaptor.getValue();
      assertEquals(recordingId.toString(), recordingUpdated.getId());
      assertEquals(meeting, recordingUpdated.getMeeting());
      assertEquals(OffsetDateTime.parse("2022-01-01T11:00:00Z"), recordingUpdated.getStartedAt());
      assertEquals(userId.toString(), recordingUpdated.getStarterId());
      assertEquals(RecordingStatus.STOPPED, recordingUpdated.getStatus());
    }
  }

  @Nested
  @DisplayName("Start recording post processing tests")
  class StartRecordingPostProcessingTests {

    @Test
    @DisplayName("Send request to video recorder for post processing")
    void startRecordingPostProcessing_testOk() {
      videoRecorderService
          .startRecordingPostProcessing(
              RecordingInfo.create()
                  .serverId("serverId")
                  .meetingId(meeting1Id.toString())
                  .accountId(userId.toString())
                  .meetingName("meeting-name")
                  .recordingName("rec-name")
                  .folderId("rec-dir-id"))
          .join();

      verify(videoRecorderClient, times(1))
          .sendVideoRecorderRequest(
              "serverId",
              meeting1Id.toString(),
              VideoRecorderRequest.create()
                  .meetingId(meeting1Id.toString())
                  .accountId(userId.toString())
                  .meetingName("meeting-name")
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .folderId("rec-dir-id")
                  .recordingName("rec-name"));
    }

    @Test
    @DisplayName("Send request to video recorder for post processing without auth token")
    void startRecordingPostProcessing_testOkWithoutAuhToken() {
      videoRecorderService
          .startRecordingPostProcessing(
              RecordingInfo.create()
                  .serverId("serverId")
                  .meetingId(meeting1Id.toString())
                  .meetingName("meeting-name")
                  .recordingName("rec-name")
                  .folderId("rec-dir-id"))
          .join();

      verify(videoRecorderClient, times(1))
          .sendVideoRecorderRequest(
              "serverId",
              meeting1Id.toString(),
              VideoRecorderRequest.create()
                  .meetingId(meeting1Id.toString())
                  .meetingName("meeting-name")
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .folderId("rec-dir-id")
                  .recordingName("rec-name"));
    }
  }
}
