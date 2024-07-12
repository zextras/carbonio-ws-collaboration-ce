// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanMeetingRepository implements MeetingRepository {

  private final Database db;

  @Inject
  public EbeanMeetingRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<Meeting> getById(String meetingId) {
    return db.find(Meeting.class)
        .fetch("participants")
        .where()
        .eq("id", meetingId)
        .findOneOrEmpty();
  }

  @Override
  public List<Meeting> getByRoomsIds(List<String> roomsIds) {
    return db.find(Meeting.class).fetch("participants").where().in("roomId", roomsIds).findList();
  }

  @Override
  public Optional<Meeting> getByRoomId(String roomId) {
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

  @Override
  public Meeting update(Meeting meeting) {
    db.update(meeting);
    return meeting;
  }

  @Override
  public void delete(Meeting meeting) {
    db.delete(meeting);
  }
}
