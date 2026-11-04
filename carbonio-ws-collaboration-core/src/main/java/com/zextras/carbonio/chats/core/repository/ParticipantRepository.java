// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository {

  Optional<Participant> getById(String meetingId, String userId);

  Optional<Participant> getByUserId(String userId);

  Optional<Participant> getByQueueId(String queueId);

  List<Participant> getByMeetingId(String meetingId);

  List<Participant> getHandRaisedByMeetingId(String meetingId);

  Participant insert(Participant participant);

  Participant update(Participant participant);

  /**
   * Raises or lowers the hand of the participant, stamping the database clock when raised
   *
   * @param participant {@link Participant} to modify
   * @param raised true to raise the hand, false to lower it
   * @return {@link Participant} refreshed with the stored times
   */
  Participant setHandRaised(Participant participant, boolean raised);

  boolean remove(Participant participant);

  void clear(String meetingId);
}
