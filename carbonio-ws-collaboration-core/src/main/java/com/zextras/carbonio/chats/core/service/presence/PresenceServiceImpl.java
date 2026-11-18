// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.presence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import java.util.Collection;
import java.util.List;

@Singleton
public class PresenceServiceImpl implements PresenceService {

  private final WsPresenceRepository wsPresenceRepository;
  private final RoomRepository roomRepository;

  @Inject
  public PresenceServiceImpl(
      WsPresenceRepository wsPresenceRepository, RoomRepository roomRepository) {
    this.wsPresenceRepository = wsPresenceRepository;
    this.roomRepository = roomRepository;
  }

  @Override
  public List<PresenceSummary> getBatchPresence(Collection<String> userIds) {
    return wsPresenceRepository.getBatchPresence(userIds);
  }

  @Override
  public List<String> getPresenceSubscribers(String userId) {
    // Offline users would drop the event anyway and load presence when they connect
    return roomRepository.getOnlineUserIdsSharingRoomWith(userId);
  }
}
