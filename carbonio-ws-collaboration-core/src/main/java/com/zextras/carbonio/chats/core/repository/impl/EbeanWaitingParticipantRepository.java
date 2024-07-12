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
import io.ebean.Query;
import io.vavr.control.Option;
import java.util.List;

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
