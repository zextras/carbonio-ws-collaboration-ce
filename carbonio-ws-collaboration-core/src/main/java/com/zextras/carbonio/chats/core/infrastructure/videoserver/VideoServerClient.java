// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;
import java.util.concurrent.CompletableFuture;

public interface VideoServerClient {

  VideoServerResponse sendGetInfoRequest();

  CompletableFuture<VideoServerResponse> sendVideoServerRequest(VideoServerMessageRequest request);

  CompletableFuture<VideoServerResponse> sendConnectionVideoServerRequest(
      String connectionId, VideoServerMessageRequest request);

  CompletableFuture<VideoServerResponse> sendHandleVideoServerRequest(
      String connectionId, String handleId, VideoServerMessageRequest request);

  CompletableFuture<AudioBridgeResponse> sendAudioBridgeRequest(
      String connectionId, String handleId, VideoServerMessageRequest request);

  CompletableFuture<VideoRoomResponse> sendVideoRoomRequest(
      String connectionId, String handleId, VideoServerMessageRequest request);
}
