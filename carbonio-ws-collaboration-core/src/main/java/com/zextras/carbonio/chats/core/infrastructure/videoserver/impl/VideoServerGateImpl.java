// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerGate;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerMonitor;

public class VideoServerGateImpl implements VideoServerGate {

  private final VideoServerMonitor videoServerMonitor;

  @Inject
  public VideoServerGateImpl(VideoServerMonitor videoServerMonitor) {
    this.videoServerMonitor = videoServerMonitor;
  }

  @Override
  public boolean isDown(VideoServerMeeting videoServerMeeting) {
    return videoServerMonitor.isDown();
  }

  @Override
  public boolean isShutDown(VideoServerMeeting videoServerMeeting) {
    return videoServerMonitor.isShutDown();
  }
}
