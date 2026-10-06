// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerPluginRequest;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link VideoServerGateway} over the CE {@link VideoServerServiceImpl} plugin-message helpers (same
 * package, so their protected access is kept). Neither this class nor the helpers are
 * transactional: only the row lookup goes through the (transactional) repository and returns an
 * {@link Optional}, so a videoserver failure never crosses a transaction interceptor and never
 * marks the caller's transaction rollback-only.
 */
@ApplicationScoped
public class VideoServerGatewayImpl implements VideoServerGateway {

  private final VideoServerServiceImpl videoServerService;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final ObjectMapper objectMapper;

  @Inject
  public VideoServerGatewayImpl(
      VideoServerServiceImpl videoServerService,
      VideoServerMeetingRepository videoServerMeetingRepository,
      ObjectMapper objectMapper) {
    this.videoServerService = videoServerService;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.objectMapper = objectMapper;
  }

  /** A plugin-owned request body, serialised as is. */
  static final class PluginBody extends VideoServerPluginRequest {
    private final Object body;

    PluginBody(Object body) {
      this.body = body;
    }

    @JsonValue
    public Object body() {
      return body;
    }
  }

  @Override
  public Optional<VideoServerRooms> roomsOf(UUID meetingId) {
    return videoServerMeetingRepository
        .getById(meetingId.toString())
        .map(v -> new VideoServerRooms(v.getServerId(), v.getAudioRoomId(), v.getVideoRoomId()));
  }

  @Override
  public JsonNode sendVideoRoomMessage(UUID meetingId, Object body) {
    VideoServerMeeting v = videoServerService.getVideoServerMeeting(meetingId.toString());
    return objectMapper.valueToTree(
        videoServerService.sendVideoRoomPluginMessage(
            v.getServerId(), v.getConnectionId(), v.getVideoHandleId(), new PluginBody(body), null));
  }

  @Override
  public JsonNode sendAudioBridgeMessage(UUID meetingId, Object body) {
    VideoServerMeeting v = videoServerService.getVideoServerMeeting(meetingId.toString());
    return objectMapper.valueToTree(
        videoServerService.sendAudioBridgePluginMessage(
            v.getServerId(), v.getConnectionId(), v.getAudioHandleId(), new PluginBody(body), null));
  }
}
