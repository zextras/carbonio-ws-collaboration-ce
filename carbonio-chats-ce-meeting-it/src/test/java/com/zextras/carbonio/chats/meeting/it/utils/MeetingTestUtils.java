package com.zextras.carbonio.chats.meeting.it.utils;

import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.it.data.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MeetingTestUtils {

  private final MeetingRepository meetingRepository;

  @Inject
  public MeetingTestUtils(MeetingRepository meetingRepository) {
    this.meetingRepository = meetingRepository;
  }

  public Meeting generateAndSaveMeeting(UUID meetingId, UUID roomId, List<ParticipantBuilder> participantBuilders) {
    Meeting meeting = Meeting.create()
      .id(meetingId.toString())
      .roomId(roomId.toString());
    meeting.participants(participantBuilders.stream().map(participantBuilder ->
        participantBuilder.build(meeting))
      .collect(Collectors.toList()));
    return meetingRepository.insert(meeting);
  }

  public Optional<Meeting> getMeetingById(UUID meetingId) {
    return meetingRepository.getMeetingById(meetingId.toString());
  }

}
