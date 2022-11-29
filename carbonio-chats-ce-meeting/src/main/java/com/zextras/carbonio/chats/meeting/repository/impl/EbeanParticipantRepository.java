package com.zextras.carbonio.chats.meeting.repository.impl;

import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.data.entity.Participant;
import com.zextras.carbonio.chats.meeting.repository.ParticipantRepository;
import io.ebean.DB;
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
}
