// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;

public interface VideoServerClient {

  VideoServerResponse sendGetInfoRequest();

  VideoServerResponse sendVideoServerRequest(String serverId, VideoServerMessageRequest request);

  VideoServerResponse sendConnectionVideoServerRequest(
      String connectionId, String serverId, VideoServerMessageRequest request);

  VideoServerResponse sendHandleVideoServerRequest(
      String connectionId, String handleId, String serverId, VideoServerMessageRequest request);

  AudioBridgeResponse sendAudioBridgeRequest(
      String connectionId, String handleId, String serverId, VideoServerMessageRequest request);

  VideoRoomResponse sendVideoRoomRequest(
      String connectionId, String handleId, String serverId, VideoServerMessageRequest request);
}
