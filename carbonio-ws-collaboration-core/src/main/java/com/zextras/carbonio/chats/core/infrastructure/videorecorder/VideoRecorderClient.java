// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder;

import com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request.VideoRecorderRequest;
import java.util.concurrent.CompletableFuture;

public interface VideoRecorderClient {

  CompletableFuture<Void> sendVideoRecorderRequest(
      String serverId, String meetingId, VideoRecorderRequest request);
}
