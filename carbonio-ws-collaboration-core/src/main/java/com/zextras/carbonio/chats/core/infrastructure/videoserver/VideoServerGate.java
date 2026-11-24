// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;

public interface VideoServerGate {

  // refuse media calls at once instead of waiting for the HTTP timeout
  boolean isDown(VideoServerMeeting videoServerMeeting);

  // skip Janus on leave and stop only once it confirmed its rooms and sessions are gone
  boolean isShutDown(VideoServerMeeting videoServerMeeting);
}
