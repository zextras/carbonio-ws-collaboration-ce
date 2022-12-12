package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_ATTACH;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_AUDIOBRIDGE_PLUGIN;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_CREATE;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_MESSAGE;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_SUCCESS;
import static com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient.JANUS_VIDEOROOM_PLUGIN;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSessionUser;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeRoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerIdResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerPluginResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionUserRepository;
import io.ebean.annotation.Transactional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoServerServiceImpl implements VideoServerService {

  private final VideoServerClient                videoServerClient;
  private final VideoServerMeetingRepository     videoServerMeetingRepository;
  private final VideoServerSessionUserRepository videoServerSessionUserRepository;

  @Inject
  public VideoServerServiceImpl(
    VideoServerClient videoServerClient,
    VideoServerMeetingRepository videoServerMeetingRepository,
    VideoServerSessionUserRepository videoServerSessionUserRepository
  ) {
    this.videoServerClient = videoServerClient;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionUserRepository = videoServerSessionUserRepository;
  }

  @Override
  @Transactional
  public void createMeeting(String meetingId) {
    if (videoServerMeetingRepository.getByMeetingId(meetingId).isPresent()) {
      throw new VideoServerException("Videoserver meeting " + meetingId + " is already present");
    }
    VideoServerResponse videoServerResponse = videoServerClient.createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection for the meeting " + meetingId);
    }
    VideoServerIdResponse response = (VideoServerIdResponse) videoServerResponse;
    String connectionId = response.getDataId();
    videoServerResponse = videoServerClient.interactWithConnection(connectionId, JANUS_ATTACH,
      JANUS_AUDIOBRIDGE_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the audiobridge plugin for the connection " + connectionId +
          " for the meeting " + meetingId);
    }
    response = (VideoServerIdResponse) videoServerResponse;
    String audioHandleId = response.getDataId();
    AudioBridgeRoomRequest audioBridgeRoomRequest = AudioBridgeRoomRequest.create(JANUS_CREATE);
    VideoServerPluginResponse audioPluginResponse = videoServerClient.sendPluginMessage(
      connectionId,
      audioHandleId,
      JANUS_MESSAGE,
      audioBridgeRoomRequest
    );
    if (!audioPluginResponse.statusOK()) {
      throw new VideoServerException(
        "An error occurred when creating an audiobridge room for the connection " + connectionId + " with plugin "
          + audioHandleId + " for the meeting " + meetingId);
    }
    videoServerResponse = videoServerClient.interactWithConnection(connectionId, JANUS_ATTACH, JANUS_VIDEOROOM_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the videoroom plugin for the connection " + connectionId
          + " for the meeting " + meetingId);
    }
    response = (VideoServerIdResponse) videoServerResponse;
    String videoHandleId = response.getDataId();
    VideoRoomRequest videoRoomRequest = VideoRoomRequest.create(JANUS_CREATE);
    VideoServerPluginResponse videoPluginResponse = videoServerClient.sendPluginMessage(
      connectionId,
      videoHandleId,
      JANUS_MESSAGE,
      videoRoomRequest
    );
    if (!videoPluginResponse.statusOK()) {
      throw new VideoServerException(
        "An error occurred when creating a videoroom room for the connection " + connectionId + " with plugin "
          + videoHandleId + " for the meeting " + meetingId);
    }
    videoServerMeetingRepository.insert(
      VideoServerMeeting.create()
        .meetingId(meetingId)
        .connectionId(connectionId)
        .audioHandleId(audioHandleId)
        .videoHandleId(videoHandleId)
        .audioRoomId(audioPluginResponse.getRoom())
        .videoRoomId(videoPluginResponse.getRoom())
    );
  }

  @Override
  @Transactional
  public void deleteMeeting(String meetingId) {
    VideoServerMeeting videoServerMeetingToRemove = videoServerMeetingRepository.getByMeetingId(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerPluginResponse videoServerPluginResponse = videoServerClient.destroyPluginHandle(
      videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getAudioHandleId());
    if (!videoServerPluginResponse.statusOK()) {
      throw new VideoServerException("An error occurred when destroying the audioroom plugin handle for the connection "
        + videoServerMeetingToRemove.getConnectionId() + " with plugin "
        + videoServerMeetingToRemove.getAudioHandleId() + " for the meeting " + meetingId);
    }
    videoServerPluginResponse = videoServerClient.destroyPluginHandle(
      videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getVideoHandleId());
    if (!videoServerPluginResponse.statusOK()) {
      throw new VideoServerException("An error occurred when destroying the videoroom plugin handle for the connection "
        + videoServerMeetingToRemove.getConnectionId() + " with plugin "
        + videoServerMeetingToRemove.getVideoHandleId() + " for the meeting " + meetingId);
    }
    VideoServerResponse videoServerResponse = videoServerClient.destroyConnection(
      videoServerMeetingToRemove.getConnectionId());
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when destroying the videoserver connection "
          + videoServerMeetingToRemove.getConnectionId() + " for the meeting " + meetingId);
    }
    videoServerMeetingRepository.deleteById(meetingId);
  }

  @Override
  @Transactional
  public void joinMeeting(String userId, String sessionId, String meetingId, boolean webcamOn, boolean audioOn) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getByMeetingId(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    if (videoServerMeeting.getVideoServerSessionUsers().stream().anyMatch(videoServerSessionUser ->
      videoServerSessionUser.getUserId().equals(userId) && videoServerSessionUser.getSessionId().equals(sessionId))) {
      throw new VideoServerException(
        "Videoserver session user with userId " + userId + " and sessionId " + sessionId
          + "is already present in the videoserver meeting " + meetingId);
    }
    VideoServerResponse videoServerResponse = videoServerClient.createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection for userId " + userId + " and sessionId " + sessionId
          + " on the meeting " + meetingId);
    }
    VideoServerIdResponse response = (VideoServerIdResponse) videoServerResponse;
    String connectionId = response.getDataId();
    videoServerResponse = videoServerClient.interactWithConnection(connectionId, JANUS_ATTACH, JANUS_VIDEOROOM_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the videoroom plugin for the connection " + connectionId + " with userId "
          + userId + " and sessionId " + sessionId + " on the meeting " + meetingId);
    }
    response = (VideoServerIdResponse) videoServerResponse;
    String videoHandleId = response.getDataId();
    videoServerSessionUserRepository.insert(
      VideoServerSessionUser.create(userId, sessionId, videoServerMeeting)
        .connectionId(connectionId)
        .videoHandleId(videoHandleId)
        .videoOn(webcamOn)
        .audioOn(audioOn)
    );
  }

  @Override
  @Transactional
  public void leaveMeeting(String userId, String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getByMeetingId(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSessionUser videoServerSessionUser = videoServerMeeting.getVideoServerSessionUsers().stream()
      .filter(sessionUser -> sessionUser.getUserId().equals(userId) && sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException("No Videoserver session user found for user "
        + userId + " with session " + sessionId + " for the meeting " + meetingId));
    VideoServerResponse videoServerResponse = videoServerClient.destroyConnection(
      videoServerSessionUser.getConnectionId());
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when destroying the videoserver connection " + videoServerSessionUser.getConnectionId()
          + " for user " + userId + " with session " + sessionId + " on the meeting " + meetingId);
    }
    videoServerSessionUserRepository.remove(videoServerSessionUser);
  }

  @Override
  public boolean isAlive() {
    return true;
  }
}
