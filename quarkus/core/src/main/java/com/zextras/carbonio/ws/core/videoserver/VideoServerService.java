// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.videoserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingEntity;
import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class VideoServerService {

  static final String AUDIOBRIDGE = "janus.plugin.audiobridge";
  static final String VIDEOROOM = "janus.plugin.videoroom";
  static final String VIDEO_CODECS = "vp8,h264,vp9,h265,av1";

  @Inject JanusClient janus;
  @Inject VideoServerConfig config;
  @Inject VideoServerMeetingRepository repository;

  @Transactional(Transactional.TxType.MANDATORY)
  public void startMeeting(String meetingId) {
    if (repository.findByIdOptional(meetingId).isPresent()) {
      return;
    }
    String serverId = config.serverId().orElse(null);
    String connectionId = janus.createConnection(serverId);
    String audioHandle = janus.attach(serverId, connectionId, AUDIOBRIDGE, "meeting/a/" + meetingId);
    String videoHandle = janus.attach(serverId, connectionId, VIDEOROOM, "meeting/v/" + meetingId);
    JsonNode audio = janus.message(serverId, connectionId, audioHandle, audioRoom(meetingId));
    requireCreated(audio, "audiobridge", meetingId);
    JsonNode video = janus.message(serverId, connectionId, videoHandle, videoRoom(meetingId));
    requireCreated(video, "videoroom", meetingId);

    VideoServerMeetingEntity entity = new VideoServerMeetingEntity();
    entity.meetingId = meetingId;
    entity.serverId = serverId;
    entity.connectionId = connectionId;
    entity.audioHandleId = audioHandle;
    entity.videoHandleId = videoHandle;
    entity.audioRoomId = audio.path("plugindata").path("data").path("room").asText();
    entity.videoRoomId = video.path("plugindata").path("data").path("room").asText();
    repository.persist(entity);
  }

  @Transactional(Transactional.TxType.MANDATORY)
  public void stopMeeting(String meetingId) {
    repository.findByIdOptional(meetingId).ifPresent(vsm -> {
      janus.message(vsm.serverId, vsm.connectionId, vsm.audioHandleId,
          Map.of("request", "destroy", "room", vsm.audioRoomId, "permanent", false));
      janus.message(vsm.serverId, vsm.connectionId, vsm.videoHandleId,
          Map.of("request", "destroy", "room", vsm.videoRoomId, "permanent", false));
      janus.detach(vsm.serverId, vsm.connectionId, vsm.audioHandleId);
      janus.detach(vsm.serverId, vsm.connectionId, vsm.videoHandleId);
      janus.destroy(vsm.serverId, vsm.connectionId);
      repository.delete(vsm);
    });
  }

  private static void requireCreated(JsonNode response, String plugin, String meetingId) {
    if (!"created".equals(JanusClient.pluginStatus(response, plugin))) {
      throw new VideoServerException(
          "An error occurred when creating a " + plugin + " room for the meeting " + meetingId);
    }
  }

  private static Map<String, Object> audioRoom(String meetingId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("request", "create");
    body.put("room", "audio_" + meetingId);
    body.put("permanent", false);
    body.put("description", "audio_room_" + meetingId);
    body.put("is_private", false);
    body.put("record", false);
    body.put("sampling_rate", 16000L);
    body.put("audio_active_packets", 10L);
    body.put("audio_level_average", 65);
    body.put("audiolevel_event", true);
    return body;
  }

  private Map<String, Object> videoRoom(String meetingId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("request", "create");
    body.put("room", "video_" + meetingId);
    body.put("permanent", false);
    body.put("description", "video_room_" + meetingId);
    body.put("is_private", false);
    body.put("record", false);
    body.put("videoorient_ext", false);
    body.put("publishers", 100);
    body.put("bitrate", (long) config.bitrate());
    body.put("bitrate_cap", config.bitrateCap());
    body.put("videocodec", VIDEO_CODECS);
    return body;
  }
}
