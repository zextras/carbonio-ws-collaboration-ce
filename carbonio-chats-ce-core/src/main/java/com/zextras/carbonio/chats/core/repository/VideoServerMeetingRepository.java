// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import java.util.Optional;

public interface VideoServerMeetingRepository {

  /**
   * Gets the {@link VideoServerMeeting} by meeting identifier
   *
   * @param meetingId meeting identifier
   * @return an {@link Optional} with the {@link VideoServerMeeting} required if it exists
   */
  Optional<VideoServerMeeting> getById(String meetingId);

  /**
   * Inserts a new {@link VideoServerMeeting}
   *
   * @param videoServerMeeting {@link VideoServerMeeting} to insert
   * @return {@link VideoServerMeeting} inserted
   */
  VideoServerMeeting insert(VideoServerMeeting videoServerMeeting);

  /**
   * Deletes a {@link VideoServerMeeting} by meeting identifier
   *
   * @param meetingId meeting identifier {@link String}
   */
  void deleteById(String meetingId);

  /**
   * Updates a {@link VideoServerMeeting}
   *
   * @param videoServerMeeting {@link VideoServerMeeting} to update
   * @return {@link VideoServerMeeting} updated
   */
  VideoServerMeeting update(VideoServerMeeting videoServerMeeting);
}
