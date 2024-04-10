// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRoomStartVideoInRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomStartVideoInRequest videoRoomStartVideoInRequest =
        VideoRoomStartVideoInRequest.create().request("start");

    assertEquals("start", videoRoomStartVideoInRequest.getRequest());
  }

  @Test
  void test_equals_ok() {
    VideoRoomStartVideoInRequest videoRoomStartVideoInRequest =
        VideoRoomStartVideoInRequest.create().request("start");

    assertEquals(
        VideoRoomStartVideoInRequest.create().request("start"), videoRoomStartVideoInRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomStartVideoInRequest videoRoomStartVideoInRequest =
        VideoRoomStartVideoInRequest.create().request("start");

    assertNotEquals(
        VideoRoomStartVideoInRequest.create().request("stop"), videoRoomStartVideoInRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomStartVideoInRequest videoRoomStartVideoInRequest =
        VideoRoomStartVideoInRequest.create().request("start");

    assertNotEquals(null, videoRoomStartVideoInRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomStartVideoInRequest videoRoomStartVideoInRequest =
        VideoRoomStartVideoInRequest.create().request("start");

    assertEquals(
        VideoRoomStartVideoInRequest.create().request("start").hashCode(),
        videoRoomStartVideoInRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomStartVideoInRequest videoRoomStartVideoInRequest =
        VideoRoomStartVideoInRequest.create().request("start");

    assertNotEquals(
        VideoRoomStartVideoInRequest.create().request("stop").hashCode(),
        videoRoomStartVideoInRequest.hashCode());
  }
}
