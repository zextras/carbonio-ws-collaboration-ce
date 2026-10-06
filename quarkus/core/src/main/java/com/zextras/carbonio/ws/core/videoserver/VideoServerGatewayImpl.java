// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.videoserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingEntity;
import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingRepository;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;

/**
 * Only the row lookup joins (or opens) a transaction; the Janus call runs outside any
 * interceptor-managed boundary. A {@link VideoServerException} therefore never marks the caller's
 * transaction rollback-only, so a plugin that catches it (e.g. recording on the MeetingStopping
 * path) really keeps core's stop/leave/delete transaction committable.
 */
@ApplicationScoped
public class VideoServerGatewayImpl implements VideoServerGateway {

  @Inject VideoServerMeetingRepository repository;
  @Inject JanusClient janus;

  @Override
  public Optional<VideoServerRooms> roomsOf(UUID meetingId) {
    return find(meetingId).map(v -> new VideoServerRooms(v.serverId, v.audioRoomId, v.videoRoomId));
  }

  @Override
  public JsonNode sendVideoRoomMessage(UUID meetingId, Object body) {
    VideoServerMeetingEntity v = require(meetingId);
    return janus.message(v.serverId, v.connectionId, v.videoHandleId, body);
  }

  @Override
  public JsonNode sendAudioBridgeMessage(UUID meetingId, Object body) {
    VideoServerMeetingEntity v = require(meetingId);
    return janus.message(v.serverId, v.connectionId, v.audioHandleId, body);
  }

  /** The only transactional part; it cannot throw a videoserver failure. */
  private Optional<VideoServerMeetingEntity> find(UUID meetingId) {
    return QuarkusTransaction.joiningExisting()
        .call(() -> repository.findByIdOptional(meetingId.toString()));
  }

  private VideoServerMeetingEntity require(UUID meetingId) {
    return find(meetingId)
        .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
  }
}
