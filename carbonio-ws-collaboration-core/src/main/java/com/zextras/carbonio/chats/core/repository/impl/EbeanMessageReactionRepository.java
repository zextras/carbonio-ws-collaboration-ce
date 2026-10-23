// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.data.entity.MessageReactionId;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanMessageReactionRepository implements MessageReactionRepository {

  private final Database db;

  @Inject
  public EbeanMessageReactionRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<MessageReaction> getByMessageId(String messageId) {
    return db.find(MessageReaction.class).where().eq("id.messageId", messageId).findList();
  }

  @Override
  public Optional<MessageReaction> getByUserAndMessage(String messageId, String userId) {
    return Optional.ofNullable(
        db.find(MessageReaction.class, new MessageReactionId(messageId, userId)));
  }

  @Override
  public MessageReaction insert(MessageReaction reaction) {
    db.insert(reaction);
    return reaction;
  }

  @Override
  public MessageReaction update(MessageReaction reaction) {
    db.update(reaction);
    return reaction;
  }

  @Override
  public void delete(String messageId, String userId) {
    db.delete(MessageReaction.class, new MessageReactionId(messageId, userId));
  }

  @Override
  public void deleteByMessageId(String messageId) {
    db.find(MessageReaction.class).where().eq("id.messageId", messageId).delete();
  }
}
