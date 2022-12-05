package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_ATTACH;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_AUDIOBRIDGE_PLUGIN;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_CREATE;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_MESSAGE;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_SUCCESS;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_VIDEOROOM_PLUGIN;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.JanusService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.AudioBridgeRoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.JanusIdResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.JanusPluginResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.JanusResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.VideoRoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.VideoServerMeeting;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.VideoServerSessionManager;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoServerServiceImpl implements JanusService {

  private final VideoServerClient               videoServerClient;
  private final Map<String, VideoServerMeeting> videoServerMeetingMap;

  @Inject
  public VideoServerServiceImpl(
    VideoServerClient videoServerClient
  ) {
    this.videoServerClient = videoServerClient;
    videoServerMeetingMap = new HashMap<>();
  }

  @Override
  public void createMeeting(String meetingId) {
    JanusResponse janusResponse = videoServerClient.createSession();
    if (!JANUS_SUCCESS.equals(janusResponse.getStatus())) {
      throw new RuntimeException();
    }
    JanusIdResponse response = (JanusIdResponse) janusResponse;
    String sessionId = response.getDataId();
    janusResponse = videoServerClient.manageSession(sessionId, JANUS_ATTACH, JANUS_AUDIOBRIDGE_PLUGIN);
    if (!JANUS_SUCCESS.equals(janusResponse.getStatus())) {
      throw new RuntimeException();
    }
    response = (JanusIdResponse) janusResponse;
    String audioHandleId = response.getDataId();
    AudioBridgeRoomRequest audioBridgeRoomRequest = new AudioBridgeRoomRequest(JANUS_CREATE);
    JanusPluginResponse audioPluginResponse = videoServerClient.sendPluginMessage(
      sessionId,
      audioHandleId,
      JANUS_MESSAGE,
      audioBridgeRoomRequest
    );
    if (!audioPluginResponse.statusOK()) {
      throw new RuntimeException();
    }
    janusResponse = videoServerClient.manageSession(sessionId, JANUS_ATTACH, JANUS_VIDEOROOM_PLUGIN);
    if (!JANUS_SUCCESS.equals(janusResponse.getStatus())) {
      throw new RuntimeException();
    }
    response = (JanusIdResponse) janusResponse;
    String videoHandleId = response.getDataId();
    VideoRoomRequest videoRoomRequest = new VideoRoomRequest(JANUS_CREATE);
    JanusPluginResponse videoPluginResponse = videoServerClient.sendPluginMessage(
      sessionId,
      videoHandleId,
      JANUS_MESSAGE,
      videoRoomRequest
    );
    if (!videoPluginResponse.statusOK()) {
      throw new RuntimeException();
    }
    videoServerMeetingMap.put(
      meetingId,
      new VideoServerMeeting(
        sessionId,
        audioHandleId,
        videoHandleId,
        audioPluginResponse.getRoom(),
        videoPluginResponse.getRoom()
      )
    );
  }

  @Override
  public void deleteMeeting(String meetingId) {
    if (!videoServerMeetingMap.containsKey(meetingId)) {
      throw new RuntimeException();
    }
    VideoServerMeeting videoServerMeeting = videoServerMeetingMap.get(meetingId);
    JanusPluginResponse janusPluginResponse = videoServerClient.destroyPluginHandle(
      videoServerMeeting.getBackendSessionId(),
      videoServerMeeting.getAudioRoomHandleId());
    if (!janusPluginResponse.statusOK()) {
      throw new RuntimeException();
    }
    janusPluginResponse = videoServerClient.destroyPluginHandle(
      videoServerMeeting.getBackendSessionId(),
      videoServerMeeting.getVideoRoomHandleId());
    if (!janusPluginResponse.statusOK()) {
      throw new RuntimeException();
    }
    JanusResponse janusResponse = videoServerClient.destroySession(videoServerMeeting.getBackendSessionId());
    if (!JANUS_SUCCESS.equals(janusResponse.getStatus())) {
      throw new RuntimeException();
    }
    videoServerMeetingMap.remove(meetingId);
  }

  @Override
  public void joinMeeting(String userId, String sessionId, String meetingId, boolean webcamOn, boolean audioOn) {
    if (!videoServerMeetingMap.containsKey(meetingId)) {
      throw new RuntimeException();
    }
    JanusResponse janusResponse = videoServerClient.createSession();
    if (!JANUS_SUCCESS.equals(janusResponse.getStatus())) {
      throw new RuntimeException();
    }
    JanusIdResponse response = (JanusIdResponse) janusResponse;
    String videoServerSessionId = response.getDataId();
    janusResponse = videoServerClient.manageSession(sessionId, JANUS_ATTACH, JANUS_VIDEOROOM_PLUGIN);
    if (!JANUS_SUCCESS.equals(janusResponse.getStatus())) {
      throw new RuntimeException();
    }
    response = (JanusIdResponse) janusResponse;
    String videoHandleId = response.getDataId();
    VideoServerMeeting videoServerMeeting = videoServerMeetingMap.get(meetingId);
    videoServerMeeting.getVideoServerSessionManagerMap().put(
      sessionId,
      new VideoServerSessionManager(
        videoServerSessionId,
        userId,
        null,
        videoHandleId,
        webcamOn,
        audioOn
      )
    );
  }

  @Override
  public void leaveMeeting(String userId, String sessionId, String meetingId) {
    if (!videoServerMeetingMap.containsKey(meetingId)) {
      throw new RuntimeException();
    }
    VideoServerMeeting videoServerMeeting = videoServerMeetingMap.get(meetingId);
    Map<String, VideoServerSessionManager> videoServerSessionManagerMap = videoServerMeeting.getVideoServerSessionManagerMap();
    if (!videoServerSessionManagerMap.containsKey(sessionId)) {
      throw new RuntimeException();
    }
    JanusResponse janusResponse = videoServerClient.destroySession(
      videoServerSessionManagerMap.get(sessionId).getVideoServerSessionId());
    if (!JANUS_SUCCESS.equals(janusResponse.getStatus())) {
      throw new RuntimeException();
    }
    videoServerSessionManagerMap.remove(sessionId);
  }
}
