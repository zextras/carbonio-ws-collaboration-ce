// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Recording;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.RecordingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MeetingTestUtils {

  private final MeetingRepository meetingRepository;
  private final RecordingRepository recordingRepository;
  private final ParticipantRepository participantRepository;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;

  @Inject
  public MeetingTestUtils(
      MeetingRepository meetingRepository,
      RecordingRepository recordingRepository,
      ParticipantRepository participantRepository,
      VideoServerMeetingRepository videoServerMeetingRepository,
      VideoServerSessionRepository videoServerSessionRepository) {
    this.meetingRepository = meetingRepository;
    this.recordingRepository = recordingRepository;
    this.participantRepository = participantRepository;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
  }

  public UUID generateAndSaveMeeting(UUID roomId, List<ParticipantBuilder> participantBuilders) {
    return generateAndSaveMeeting(
        roomId, MeetingType.PERMANENT, participantBuilders, false, List.of());
  }

  public UUID generateAndSaveMeeting(
      UUID roomId,
      MeetingType meetingType,
      List<ParticipantBuilder> participantBuilders,
      Boolean active,
      List<Recording> recordings) {
    Meeting meeting =
        meetingRepository.insert(
            Meeting.create()
                .id(UUID.randomUUID().toString())
                .name("Test Meeting for " + roomId.toString())
                .meetingType(meetingType)
                .roomId(roomId.toString()));
    recordings.forEach(recording -> recordingRepository.insert(recording.meeting(meeting)));
    meeting
        .participants(
            participantBuilders.stream()
                .map(participantBuilder -> participantBuilder.build(meeting))
                .toList())
        .active(active)
        .recordings(recordings);
    meetingRepository.update(meeting);
    return UUID.fromString(meeting.getId());
  }

  public Optional<Meeting> getMeetingById(UUID meetingId) {
    return meetingRepository.getById(meetingId.toString());
  }

  public Optional<Participant> getParticipant(UUID meetingId, String userId) {
    return participantRepository.getById(meetingId.toString(), userId);
  }

  public VideoServerMeeting insertVideoServerMeeting(
      String meetingId,
      String connectionId,
      String audioHandleId,
      String videoHandleId,
      String audioRoomId,
      String videoRoomId) {
    return videoServerMeetingRepository.insert(
        VideoServerMeeting.create()
            .serverId(UUID.randomUUID().toString())
            .meetingId(meetingId)
            .connectionId(connectionId)
            .audioHandleId(audioHandleId)
            .videoHandleId(videoHandleId)
            .audioRoomId(audioRoomId)
            .videoRoomId(videoRoomId));
  }

  public VideoServerSession insertVideoServerSession(
      VideoServerMeeting videoServerMeeting,
      String userId,
      String queueId,
      String connectionId,
      String videoOutHandleId,
      String videoInHandleId,
      String screenHandleId) {
    return videoServerSessionRepository.insert(
        VideoServerSession.create(userId, queueId, videoServerMeeting)
            .connectionId(connectionId)
            .videoOutHandleId(videoOutHandleId)
            .videoInHandleId(videoInHandleId)
            .screenHandleId(screenHandleId));
  }

  public VideoServerSession updateVideoServerSession(VideoServerSession videoServerSession) {
    return videoServerSessionRepository.update(videoServerSession);
  }
}
