package com.zextras.carbonio.chats.it.utils;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MeetingTestUtils {

  private final MeetingRepository     meetingRepository;
  private final ParticipantRepository participantRepository;

  @Inject
  public MeetingTestUtils(
    MeetingRepository meetingRepository,
    ParticipantRepository participantRepository
  ) {
    this.meetingRepository = meetingRepository;
    this.participantRepository = participantRepository;
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
    return meetingRepository.getById(meetingId.toString());
  }

  public Optional<Participant> getParticipant(UUID meetingId, String sessionId) {
    return
      participantRepository.getById(meetingId.toString(), sessionId);
  }

}
