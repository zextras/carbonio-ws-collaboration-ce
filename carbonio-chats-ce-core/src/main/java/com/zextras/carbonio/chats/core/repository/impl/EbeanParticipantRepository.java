package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import io.ebean.Database;
import java.util.List;
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
  public List<Participant> getParticipantsByMeetingId(String meetingId) {
    return db.find(Participant.class)
      .where()
      .eq("id.meetingId", meetingId)
      .findList();
  }

  @Override
  public Participant insertParticipant(Participant participant) {
    db.insert(participant);
    return participant;
  }

  @Override
  public boolean removeParticipant(Participant participant) {
    return db.delete(participant);
  }
}
