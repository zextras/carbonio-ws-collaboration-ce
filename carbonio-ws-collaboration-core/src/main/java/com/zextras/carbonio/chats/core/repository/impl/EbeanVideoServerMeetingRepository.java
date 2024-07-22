// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import io.ebean.Database;
import java.util.Optional;

@Singleton
public class EbeanVideoServerMeetingRepository implements VideoServerMeetingRepository {

  private final Database db;

  @Inject
  public EbeanVideoServerMeetingRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<VideoServerMeeting> getById(String meetingId) {
    return db.find(VideoServerMeeting.class)
        .fetch("videoServerSessions")
        .where()
        .eq("meetingId", meetingId)
        .findOneOrEmpty();
  }

  @Override
  public VideoServerMeeting insert(VideoServerMeeting videoServerMeeting) {
    db.insert(videoServerMeeting);
    return videoServerMeeting;
  }

  @Override
  public void deleteById(String meetingId) {
    db.delete(VideoServerMeeting.class, meetingId);
  }

  @Override
  public VideoServerMeeting update(VideoServerMeeting videoServerMeeting) {
    db.update(videoServerMeeting);
    return videoServerMeeting;
  }
}
