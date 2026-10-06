// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import java.util.List;
import java.util.Optional;

public interface VideoServerSessionRepository {

  Optional<VideoServerSession> getById(String userId, String meetingId);

  Optional<VideoServerSession> getByConnectionId(String connectionId);

  List<VideoServerSession> getByMeetingId(String meetingId);

  VideoServerSession insert(VideoServerSession videoServerSession);

  boolean remove(VideoServerSession videoServerSession);

  VideoServerSession update(VideoServerSession videoServerSession);
}
