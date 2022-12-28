// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import io.ebean.Database;
import java.util.List;
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
  public List<VideoServerSession> getByMeetingId(String meetingId) {
    return db.find(VideoServerSession.class)
      .where()
      .eq("id.meetingId", meetingId)
      .findList();
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
