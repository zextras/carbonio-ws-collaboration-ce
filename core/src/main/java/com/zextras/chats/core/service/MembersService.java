package com.zextras.chats.core.service;

import com.zextras.chats.core.data.entity.Room;
import java.util.UUID;

public interface MembersService {

  void setOwner(Room room, UUID userId, boolean isOwner);

}
