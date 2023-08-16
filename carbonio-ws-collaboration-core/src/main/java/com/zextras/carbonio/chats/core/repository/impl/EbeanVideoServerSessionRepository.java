// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EbeanVideoServerSessionRepository implements VideoServerSessionRepository {

  private final Database db;

  @Inject
  public EbeanVideoServerSessionRepository(Database db) {
    this.db = db;
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
    return db.find(VideoServerSession.class)
      .where()
      .eq("id.meetingId", meetingId)
      .findList();
  }

  @Override
  public VideoServerSession insert(VideoServerMeeting videoServerMeeting, String userId, String queueId, String connectionId,
    String videoOutHandleId, String screenHandleId) {
    VideoServerSession videoServerSession = VideoServerSession.create(userId, queueId, videoServerMeeting)
      .connectionId(connectionId)
      .videoOutHandleId(videoOutHandleId).screenHandleId(screenHandleId);
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
