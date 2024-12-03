// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.WaitingParticipant;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.QueueUpdateStatusDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitingParticipantService {

  void addQueuedUser(String meetingId, String userId, String queueId);

  void removeQueuedUser(WaitingParticipant waitingParticipant);

  void updateQueuedUser(WaitingParticipant waitingParticipant);

  Optional<WaitingParticipant> getWaitingParticipant(String meetingId, String userId);

  List<UUID> getQueue(UUID meetingId);

  void clearQueue(UUID meetingId);

  void removeFromQueue(UUID queueId);

  void updateQueue(
      UUID meetingId, UUID userId, QueueUpdateStatusDto status, UserPrincipal currentUser);
}
