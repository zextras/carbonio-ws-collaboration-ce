// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import java.util.Optional;

public interface VideoServerMeetingRepository {

  Optional<VideoServerMeeting> getById(String meetingId);

  VideoServerMeeting insert(VideoServerMeeting videoServerMeeting);

  void deleteById(String meetingId);

  VideoServerMeeting update(VideoServerMeeting videoServerMeeting);
}
