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

  Participant insert(Participant participant);

  Participant update(Participant participant);

  boolean remove(Participant participant);

  void clear(String meetingId);
}
