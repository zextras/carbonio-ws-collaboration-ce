// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.WaitingParticipant;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.repository.WaitingParticipantRepository;
import io.ebean.Database;
import io.ebean.Query;
import io.vavr.control.Option;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

@Singleton
public class EbeanWaitingParticipantRepository implements WaitingParticipantRepository {

  private final Database db;

  @Inject
  public EbeanWaitingParticipantRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<WaitingParticipant> find(String meetingId, String userId, JoinStatus status) {
    Query<WaitingParticipant> query = db.find(WaitingParticipant.class);
    Option.of(meetingId).map(mId -> query.where().eq("meetingId", meetingId));
    Option.of(userId).map(mId -> query.where().eq("userId", userId));
    Option.of(status).map(s -> query.where().eq("status", status.toString()));
    return query.findList();
  }

  @Override
  public WaitingParticipant insert(String meetingId, String userId, String queueId) {
    WaitingParticipant wp =
        new WaitingParticipant()
            .id(UUID.randomUUID().toString())
            .meetingId(meetingId)
            .userId(userId)
            .queueId(queueId)
            .status(JoinStatus.WAITING);
    db.save(wp);
    return wp;
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

  public void clear(String meetingId) {
    db.find(WaitingParticipant.class).where().eq("meetingId", meetingId).delete();
  }
}
