package com.zextras.carbonio.chats.meeting.repository.impl;

import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import io.ebean.Database;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MeetingRepositoryImpl implements MeetingRepository {

  private final Database db;

  @Inject
  public MeetingRepositoryImpl(Database db) {
    this.db = db;
  }

  @Override
  public Optional<Meeting> getMeetingById(String meetingId) {
    return db.find(Meeting.class)
      .fetch("participants")
      .where()
      .eq("id", meetingId)
      .findOneOrEmpty();
  }

  @Override
  public Optional<Meeting> getMeetingByRoomId(String roomId) {
    return db.find(Meeting.class)
      .fetch("participants")
      .where()
      .eq("roomId", roomId)
      .findOneOrEmpty();
  }

  @Override
  public Meeting insert(Meeting meeting) {
    db.insert(meeting);
    return meeting;
  }

}
