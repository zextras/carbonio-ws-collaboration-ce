// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.WaitingParticipant;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import java.util.List;

public interface WaitingParticipantRepository {

  List<WaitingParticipant> find(String meetingId, String userId, JoinStatus status);

  WaitingParticipant insert(WaitingParticipant waitingParticipant);

  WaitingParticipant update(WaitingParticipant waitingParticipant);

  boolean remove(WaitingParticipant waitingParticipant);

  void clear(String meetingId);
}
