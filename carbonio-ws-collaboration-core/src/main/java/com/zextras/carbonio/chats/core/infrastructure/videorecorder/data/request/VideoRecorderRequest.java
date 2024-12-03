// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents a request sent to the Video Recorder to start the post-processing phase of
 * a meeting recorded.
 *
 * <p>Its parameters are:
 *
 * <ul>
 *   <li>meeting_id: the identifier of the meeting recorded
 *   <li>meeting_name: the name of the meeting recorded
 *   <li>audio_active_packets: the value of audio active packets used for this meeting
 *   <li>audio_level_average: the value of audio level average used for this meeting
 *   <li>auth_token: the token needed to save the recording on Files
 *   <li>folder_id: the folder id where the recording will be saved on Files
 *   <li>recordingName: the name used to save the recording on Files
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/recordings.html">JanusRecordings</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRecorderRequest {

  private String meetingId;
  private String meetingName;
  private Long audioActivePackets;
  private Integer audioLevelAverage;
  private String authToken;
  private String folderId;
  private String recordingName;

  public static VideoRecorderRequest create() {
    return new VideoRecorderRequest();
  }

  public String getMeetingId() {
    return meetingId;
  }

  public VideoRecorderRequest meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public String getMeetingName() {
    return meetingName;
  }

  public VideoRecorderRequest meetingName(String meetingName) {
    this.meetingName = meetingName;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public VideoRecorderRequest audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public Integer getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRecorderRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public String getAuthToken() {
    return authToken;
  }

  public VideoRecorderRequest authToken(String authToken) {
    this.authToken = authToken;
    return this;
  }

  public String getFolderId() {
    return folderId;
  }

  public VideoRecorderRequest folderId(String folderId) {
    this.folderId = folderId;
    return this;
  }

  public String getRecordingName() {
    return recordingName;
  }

  public VideoRecorderRequest recordingName(String recordingName) {
    this.recordingName = recordingName;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRecorderRequest that)) return false;
    return Objects.equals(getMeetingId(), that.getMeetingId())
        && Objects.equals(getMeetingName(), that.getMeetingName())
        && Objects.equals(getAudioActivePackets(), that.getAudioActivePackets())
        && Objects.equals(getAudioLevelAverage(), that.getAudioLevelAverage())
        && Objects.equals(getAuthToken(), that.getAuthToken())
        && Objects.equals(getFolderId(), that.getFolderId())
        && Objects.equals(getRecordingName(), that.getRecordingName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getMeetingId(),
        getMeetingName(),
        getAudioActivePackets(),
        getAudioLevelAverage(),
        getAuthToken(),
        getFolderId(),
        getRecordingName());
  }
}
