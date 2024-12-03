// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder.impl;

import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderConfig;

public class VideoRecorderConfigImpl implements VideoRecorderConfig {

  private final String recordingPath;

  public VideoRecorderConfigImpl(String recordingPath) {
    this.recordingPath = recordingPath;
  }

  @Override
  public String getRecordingPath() {
    return recordingPath;
  }
}
