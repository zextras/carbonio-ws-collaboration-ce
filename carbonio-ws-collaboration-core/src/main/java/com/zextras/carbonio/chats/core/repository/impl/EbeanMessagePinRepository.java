// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.repository.DatabaseClock;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import io.ebean.Database;
import java.util.List;

@Singleton
public class EbeanMessagePinRepository implements MessagePinRepository {

  private final Database db;
  private final DatabaseClock databaseClock;

  @Inject
  public EbeanMessagePinRepository(Database db, DatabaseClock databaseClock) {
    this.db = db;
    this.databaseClock = databaseClock;
  }

  @Override
  public MessagePin pin(String roomId, String messageId, String pinnedBy) {
    // A room has a single pin: drop any existing pin before inserting the new one.
    db.find(MessagePin.class).where().eq("roomId", roomId).delete();
    MessagePin pin = MessagePin.create(roomId, messageId, pinnedBy).pinnedAt(databaseClock.now());
    db.insert(pin);
    return pin;
  }

  @Override
  public int unpin(String roomId, String messageId) {
    return db.find(MessagePin.class)
        .where()
        .eq("roomId", roomId)
        .eq("messageId", messageId)
        .delete();
  }

  @Override
  public List<MessagePin> getPinnedMessages(String roomId) {
    return db.find(MessagePin.class).fetch("message").where().eq("roomId", roomId).findList();
  }

  @Override
  public boolean isPinned(String messageId) {
    return db.find(MessagePin.class).where().eq("messageId", messageId).exists();
  }
}
