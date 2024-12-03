// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.WaitingParticipant;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.repository.WaitingParticipantRepository;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanWaitingParticipantRepository implements WaitingParticipantRepository {

  private final Database db;

  @Inject
  public EbeanWaitingParticipantRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<WaitingParticipant> getWaitingParticipant(String meetingId, String userId) {
    return db.find(WaitingParticipant.class)
        .where()
        .eq("meetingId", meetingId)
        .where()
        .eq("userId", userId)
        .findOneOrEmpty();
  }

  @Override
  public List<WaitingParticipant> getWaitingList(String meetingId) {
    return db.find(WaitingParticipant.class)
        .where()
        .eq("meetingId", meetingId)
        .where()
        .eq("status", JoinStatus.WAITING)
        .findList();
  }

  @Override
  public Optional<WaitingParticipant> getByQueueId(String queueId) {
    return db.find(WaitingParticipant.class).where().eq("queue_id", queueId).findOneOrEmpty();
  }

  @Override
  public WaitingParticipant insert(WaitingParticipant waitingParticipant) {
    db.save(waitingParticipant);
    return waitingParticipant;
  }

  @Override
  public WaitingParticipant update(WaitingParticipant waitingParticipant) {
    db.update(waitingParticipant);
    return waitingParticipant;
  }

  @Override
  public boolean remove(WaitingParticipant waitingParticipant) {
    return db.delete(waitingParticipant);
  }

  @Override
  public void clear(String meetingId) {
    db.find(WaitingParticipant.class).where().eq("meetingId", meetingId).delete();
  }
}
