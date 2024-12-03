// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Recording;
import com.zextras.carbonio.chats.core.data.model.RecordingInfo;
import java.util.concurrent.CompletableFuture;

public interface VideoRecorderService {

  void saveRecordingStarted(Meeting meeting, String userId, String token);

  void saveRecordingStopped(Recording recording);

  CompletableFuture<Void> startRecordingPostProcessing(RecordingInfo recordingInfo);
}
