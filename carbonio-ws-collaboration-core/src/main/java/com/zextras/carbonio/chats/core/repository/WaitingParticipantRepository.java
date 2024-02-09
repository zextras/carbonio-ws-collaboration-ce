// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.WaitingParticipant;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;

import java.util.List;
import java.util.Optional;

public interface WaitingParticipantRepository {

  /**
   * Retrieves the queued participants in a meeting
   * All parameters are optional and can be used to filter the search
   *
   * @param meetingId meeting identifier
   * @param userId    user identifier
   * @return required {@link WaitingParticipant} wrapped in a {@link Optional}
   */
  List<WaitingParticipant> find(String meetingId, String userId, JoinStatus status);

  /**
   * Inserts a new {@link WaitingParticipant}
   *
   * @param meetingId meeting identifier
   * @param userId user identifier
   * @param queueId user identifier
   * @return {@link WaitingParticipant} inserted
   */
  WaitingParticipant insert(String meetingId, String userId, String queueId);

  /**
   * Updates the {@link WaitingParticipant}
   *
   * @param waitingParticipant {@link WaitingParticipant} to update
   * @return {@link WaitingParticipant} updated
   */
  WaitingParticipant update(WaitingParticipant waitingParticipant);

  /**
   * Removes the meeting participant
   *
   * @param waitingParticipant meeting participant to remove
   * @return true if the deletion was successful, false otherwise
   */
  boolean remove(WaitingParticipant waitingParticipant);

  /**
   *
   * @param meetingId the id of the meeting to clear the queue of
   */
  void clear(String meetingId);
}
