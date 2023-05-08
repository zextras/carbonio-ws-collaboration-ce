// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.ParticipantId;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EbeanParticipantRepository implements ParticipantRepository {

  private final Database db;

  @Inject
  public EbeanParticipantRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<Participant> getById(String meetingId, String sessionId) {
    return Optional.ofNullable(
      db.find(Participant.class, ParticipantId.create(meetingId, sessionId)));
  }

  @Override
  public List<Participant> getByMeetingId(String meetingId) {
    return db.find(Participant.class)
      .where()
      .eq("id.meetingId", meetingId)
      .findList();
  }

  @Override
  public Participant insert(Participant participant) {
    db.insert(participant);
    return participant;
  }

  @Override
  public Participant update(Participant participant) {
    db.update(participant);
    return participant;
  }

  @Override
  public boolean remove(Participant participant) {
    return db.delete(participant);
  }
}