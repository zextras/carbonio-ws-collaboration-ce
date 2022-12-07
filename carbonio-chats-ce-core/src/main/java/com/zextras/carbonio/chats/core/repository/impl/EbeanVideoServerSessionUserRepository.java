package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.VideoServerSessionUser;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionUserRepository;
import io.ebean.Database;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EbeanVideoServerSessionUserRepository implements VideoServerSessionUserRepository {

  private final Database db;

  @Inject
  public EbeanVideoServerSessionUserRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<VideoServerSessionUser> getByMeetingId(String meetingId) {
    return db.find(VideoServerSessionUser.class)
      .where()
      .eq("id.meetingId", meetingId)
      .findList();
  }

  @Override
  public VideoServerSessionUser insert(VideoServerSessionUser videoServerSessionUser) {
    db.insert(videoServerSessionUser);
    return videoServerSessionUser;
  }

  @Override
  public boolean remove(VideoServerSessionUser videoServerSessionUser) {
    return db.delete(videoServerSessionUser);
  }
}
