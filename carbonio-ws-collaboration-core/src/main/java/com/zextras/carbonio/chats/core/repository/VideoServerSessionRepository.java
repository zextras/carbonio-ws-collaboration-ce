// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import java.util.List;

public interface VideoServerSessionRepository {

  /**
   * Retrieves the list of {@link VideoServerSession} for the required meeting
   *
   * @param meetingId meeting identifier
   * @return the {@link List} of the video server sessions {@link VideoServerSession}
   */
  List<VideoServerSession> getByMeetingId(String meetingId);

  /**
   * Inserts a new {@link VideoServerSession}
   *
   * @param videoServerMeeting the associated video server meeting
   * @param sessionId          the session identifier
   * @param connectionId       the connection identifier related to videoserver
   * @param videoOutHandleId   the video handle identifier related to video room plugin
   * @param screenHandleId     the screen handle identifier related to video room plugin
   * @return {@link VideoServerSession} inserted
   */
  VideoServerSession insert(VideoServerMeeting videoServerMeeting, String sessionId, String connectionId,
    String videoOutHandleId, String screenHandleId);

  /**
   * Removes a {@link VideoServerSession}
   *
   * @param videoServerSession video server session to remove
   * @return true if the deletion was successful, false otherwise
   */
  boolean remove(VideoServerSession videoServerSession);

  /**
   * Updates a {@link VideoServerSession}
   *
   * @param videoServerSession {@link VideoServerSession} to update
   * @return {@link VideoServerSession} updated
   */
  VideoServerSession update(VideoServerSession videoServerSession);
}
