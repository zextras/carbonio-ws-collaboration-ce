// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import io.ebean.Database;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

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
  public VideoServerMeeting insert(
      UUID serverId,
      String meetingId,
      String connectionId,
      String audioHandleId,
      String videoHandleId,
      String audioRoomId,
      String videoRoomId) {
    VideoServerMeeting videoServerMeeting =
        VideoServerMeeting.create()
            .serverId(serverId.toString())
            .meetingId(meetingId)
            .connectionId(connectionId)
            .audioHandleId(audioHandleId)
            .videoHandleId(videoHandleId)
            .audioRoomId(audioRoomId)
            .videoRoomId(videoRoomId);
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
