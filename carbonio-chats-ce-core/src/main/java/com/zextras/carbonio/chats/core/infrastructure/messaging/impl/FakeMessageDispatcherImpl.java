package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;

public class FakeMessageDispatcherImpl implements MessageDispatcher {

  @Override
  public boolean isAlive() {
    return false;
  }

  @Override
  public void createRoom(Room room, String senderId) {

  }

  @Override
  public void deleteRoom(String roomId, String userId) {

  }

  @Override
  public void addRoomMember(String roomId, String senderId, String recipientId) {

  }

  @Override
  public void removeRoomMember(String roomId, String senderId, String recipientId) {

  }

  @Override
  public void setMemberRole(String roomId, String senderId, String recipientId, boolean isOwner) {

  }

  @Override
  public void sendMessageToRoom(String roomId, String senderId, String message) {

  }
}
