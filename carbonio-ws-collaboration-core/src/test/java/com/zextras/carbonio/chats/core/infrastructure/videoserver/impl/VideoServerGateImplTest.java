// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerGate;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerMonitor;
import org.junit.jupiter.api.Test;

@UnitTest
class VideoServerGateImplTest {

  private final VideoServerMonitor videoServerMonitor = mock(VideoServerMonitor.class);
  private final VideoServerGate videoServerGate = new VideoServerGateImpl(videoServerMonitor);
  private final VideoServerMeeting videoServerMeeting =
      VideoServerMeeting.create().meetingId("meeting-id");

  @Test
  void isDown_followsTheVideoServer() {
    assertFalse(videoServerGate.isDown(videoServerMeeting));

    when(videoServerMonitor.isDown()).thenReturn(true);

    assertTrue(videoServerGate.isDown(videoServerMeeting));
  }

  @Test
  void isShutDown_followsTheVideoServer() {
    assertFalse(videoServerGate.isShutDown(videoServerMeeting));

    when(videoServerMonitor.isShutDown()).thenReturn(true);

    assertTrue(videoServerGate.isShutDown(videoServerMeeting));
  }
}
