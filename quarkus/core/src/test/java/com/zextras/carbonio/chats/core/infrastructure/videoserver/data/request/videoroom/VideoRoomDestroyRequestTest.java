// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRoomDestroyRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomDestroyRequest videoRoomDestroyRequest =
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id").permanent(false);

    assertEquals("destroy", videoRoomDestroyRequest.getRequest());
    assertEquals("video-room-id", videoRoomDestroyRequest.getRoom());
    assertFalse(videoRoomDestroyRequest.getPermanent());
  }

  @Test
  void test_equals_ok() {
    VideoRoomDestroyRequest videoRoomDestroyRequest =
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id").permanent(false);

    assertEquals(
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id").permanent(false),
        videoRoomDestroyRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomDestroyRequest videoRoomDestroyRequest =
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id");

    assertNotEquals(
        VideoRoomDestroyRequest.create().request("destroy").room("room-id"),
        videoRoomDestroyRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomDestroyRequest videoRoomDestroyRequest =
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id");

    assertNotEquals(null, videoRoomDestroyRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomDestroyRequest videoRoomDestroyRequest =
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id");

    assertEquals(
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id").hashCode(),
        videoRoomDestroyRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomDestroyRequest videoRoomDestroyRequest =
        VideoRoomDestroyRequest.create().request("destroy").room("video-room-id");

    assertNotEquals(
        VideoRoomDestroyRequest.create().request("destroy").room("room-id").hashCode(),
        videoRoomDestroyRequest.hashCode());
  }
}
