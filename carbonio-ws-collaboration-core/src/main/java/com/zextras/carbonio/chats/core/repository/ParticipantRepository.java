// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository {

  /**
   * Retrieves a participant by its session identifier
   *
   * @param sessionId session identifier
   * @return required {@link Participant} wrapped in a {@link Optional}
   */
  Optional<Participant> getByUserId(String sessionId);

  /**
   * Retrieves a participant by its row identifier
   *
   * @param meetingId meeting identifier
   * @param sessionId session identifier
   * @return required {@link Participant} wrapped in a {@link Optional}
   */
  Optional<Participant> getById(String meetingId, String sessionId);

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
   * Updates the {@link Participant}
   *
   * @param participant {@link Participant} to update
   * @return {@link Participant} updated
   */
  Participant update(Participant participant);

  /**
   * Removes the meeting participant
   *
   * @param participant meeting participant to remove
   * @return true if the deletion was successful, false otherwise
   */
  boolean remove(Participant participant);
}
