// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.videoserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingEntity;
import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingRepository;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VideoServerGatewayImpl implements VideoServerGateway {

  @Inject VideoServerMeetingRepository repository;
  @Inject JanusClient janus;

  @Override
  @Transactional
  public Optional<VideoServerRooms> roomsOf(UUID meetingId) {
    return repository.findByIdOptional(meetingId.toString())
        .map(v -> new VideoServerRooms(v.serverId, v.audioRoomId, v.videoRoomId));
  }

  @Override
  @Transactional
  public JsonNode sendVideoRoomMessage(UUID meetingId, Object body) {
    VideoServerMeetingEntity v = require(meetingId);
    return janus.message(v.serverId, v.connectionId, v.videoHandleId, body);
  }

  @Override
  @Transactional
  public JsonNode sendAudioBridgeMessage(UUID meetingId, Object body) {
    VideoServerMeetingEntity v = require(meetingId);
    return janus.message(v.serverId, v.connectionId, v.audioHandleId, body);
  }

  private VideoServerMeetingEntity require(UUID meetingId) {
    return repository.findByIdOptional(meetingId.toString())
        .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
  }
}
