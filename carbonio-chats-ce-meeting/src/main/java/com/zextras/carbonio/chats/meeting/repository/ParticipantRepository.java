package com.zextras.carbonio.chats.meeting.repository;

import com.zextras.carbonio.chats.meeting.data.entity.Participant;
import java.util.List;

public interface ParticipantRepository {

  /**
   * Retrieves the list of meeting participants
   *
   * @param meetingId meeting identifier
   * @return the {@link List} of the meeting {@link Participant}
   */
  List<Participant> getParticipantsByMeetingId(String meetingId);
}
