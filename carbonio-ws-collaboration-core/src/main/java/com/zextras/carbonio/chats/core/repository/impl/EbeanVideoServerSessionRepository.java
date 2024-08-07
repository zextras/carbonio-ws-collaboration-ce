// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSessionId;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanVideoServerSessionRepository implements VideoServerSessionRepository {

  private final Database db;

  @Inject
  public EbeanVideoServerSessionRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<VideoServerSession> getById(String userId, String meetingId) {
    return Optional.ofNullable(
        db.find(VideoServerSession.class, new VideoServerSessionId(userId, meetingId)));
  }

  @Override
  public Optional<VideoServerSession> getByConnectionId(String connectionId) {
    return db.find(VideoServerSession.class)
        .where()
        .eq("connection_id", connectionId)
        .findOneOrEmpty();
  }

  @Override
  public List<VideoServerSession> getByMeetingId(String meetingId) {
    return db.find(VideoServerSession.class).where().eq("id.meetingId", meetingId).findList();
  }

  @Override
  public VideoServerSession insert(VideoServerSession videoServerSession) {
    db.insert(videoServerSession);
    return videoServerSession;
  }

  @Override
  public boolean remove(VideoServerSession videoServerSession) {
    return db.delete(videoServerSession);
  }

  @Override
  public VideoServerSession update(VideoServerSession videoServerSession) {
    db.update(videoServerSession);
    return videoServerSession;
  }
}
