package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import java.util.List;

public interface ParticipantRepository {

  /**
   * Retrieves a participant by its row identifier
   *
   * @param userId    user identifier
   * @param meetingId meeting identifier
   * @param sessionId session identifier
   * @return required {@link Participant} if it exists else null
   */
  Participant getById(String userId, String meetingId, String sessionId);

  /**
   * Retrieves the list of meeting participants
   *
   * @param meetingId meeting identifier
   * @return the {@link List} of the meeting {@link Participant}
   */
  List<Participant> getByMeetingId(String meetingId);

  /**
   * Inserts a new {@link Participant}
   *
   * @param participant {@link Participant} to insert
   * @return {@link Participant} inserted
   */
  Participant insert(Participant participant);

  /**
   * Removes the meeting participant
   *
   * @param participant meeting participant to remove
   * @return true if the deletion was successful, false otherwise
   */
  boolean remove(Participant participant);
}
