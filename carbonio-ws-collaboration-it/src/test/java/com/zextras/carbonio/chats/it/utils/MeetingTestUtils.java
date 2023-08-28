// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.utils;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MeetingTestUtils {

  private final MeetingRepository            meetingRepository;
  private final ParticipantRepository        participantRepository;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;

  @Inject
  public MeetingTestUtils(
    MeetingRepository meetingRepository,
    ParticipantRepository participantRepository,
    VideoServerMeetingRepository videoServerMeetingRepository,
    VideoServerSessionRepository videoServerSessionRepository
  ) {
    this.meetingRepository = meetingRepository;
    this.participantRepository = participantRepository;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
  }

  public UUID generateAndSaveMeeting(UUID roomId, List<ParticipantBuilder> participantBuilders) {
    return generateAndSaveMeeting(roomId, participantBuilders, false, null);
  }

  public UUID generateAndSaveMeeting(
    UUID roomId,
    List<ParticipantBuilder> participantBuilders,
    Boolean active,
    OffsetDateTime expiration
  ) {
    Meeting meeting = meetingRepository.insert("Test Meeting for " + roomId.toString(),
      MeetingType.PERMANENT,
      roomId,
      expiration);
    meeting.participants(participantBuilders.stream().map(participantBuilder ->
        participantBuilder.build(meeting))
      .collect(Collectors.toList()));
    meeting.active(active);
    meetingRepository.update(meeting);
    return UUID.fromString(meeting.getId());
  }

  public Optional<Meeting> getMeetingById(UUID meetingId) {
    return meetingRepository.getById(meetingId.toString());
  }

  public Optional<Participant> getParticipant(UUID meetingId, String sessionId) {
    return participantRepository.getById(meetingId.toString(), sessionId);
  }

  public VideoServerMeeting insertVideoServerMeeting(
    String meetingId, String connectionId, String audioHandleId, String videoHandleId, String audioRoomId,
    String videoRoomId
  ) {
    return videoServerMeetingRepository.insert(meetingId, connectionId, audioHandleId,
      videoHandleId, audioRoomId, videoRoomId);
  }

  public VideoServerSession insertVideoServerSession(
    VideoServerMeeting videoServerMeeting, String sessionId, String connectionId,
    String videoOutHandleId, String screenHandleId
  ) {
    return videoServerSessionRepository.insert(videoServerMeeting, sessionId, connectionId,
      videoOutHandleId, screenHandleId);
  }

  public VideoServerSession updateVideoServerSession(VideoServerSession videoServerSession) {
    return videoServerSessionRepository.update(videoServerSession);
  }

}
