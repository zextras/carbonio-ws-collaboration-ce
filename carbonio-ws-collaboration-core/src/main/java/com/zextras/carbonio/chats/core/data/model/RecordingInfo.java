// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import java.util.Objects;

public class RecordingInfo {

  private String serverId;
  private String meetingId;
  private String meetingName;
  private String folderId;
  private String recordingName;
  private String recordingToken;

  public static RecordingInfo create() {
    return new RecordingInfo();
  }

  public String getServerId() {
    return serverId;
  }

  public RecordingInfo serverId(String serverId) {
    this.serverId = serverId;
    return this;
  }

  public String getMeetingId() {
    return meetingId;
  }

  public RecordingInfo meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public String getMeetingName() {
    return meetingName;
  }

  public RecordingInfo meetingName(String meetingName) {
    this.meetingName = meetingName;
    return this;
  }

  public String getFolderId() {
    return folderId;
  }

  public RecordingInfo folderId(String folderId) {
    this.folderId = folderId;
    return this;
  }

  public String getRecordingName() {
    return recordingName;
  }

  public RecordingInfo recordingName(String recordingName) {
    this.recordingName = recordingName;
    return this;
  }

  public String getRecordingToken() {
    return recordingToken;
  }

  public RecordingInfo recordingToken(String authToken) {
    this.recordingToken = authToken;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof RecordingInfo that)) return false;
    return Objects.equals(getServerId(), that.getServerId())
        && Objects.equals(getMeetingId(), that.getMeetingId())
        && Objects.equals(getMeetingName(), that.getMeetingName())
        && Objects.equals(getFolderId(), that.getFolderId())
        && Objects.equals(getRecordingName(), that.getRecordingName())
        && Objects.equals(getRecordingToken(), that.getRecordingToken());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getServerId(),
        getMeetingId(),
        getMeetingName(),
        getFolderId(),
        getRecordingName(),
        getRecordingToken());
  }
}
