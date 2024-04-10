// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRoomEditRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomEditRequest videoRoomEditRequest =
        VideoRoomEditRequest.create()
            .request("edit")
            .room("video-room-id")
            .newRecDir("new-rec-dir");

    assertEquals("edit", videoRoomEditRequest.getRequest());
    assertEquals("video-room-id", videoRoomEditRequest.getRoom());
    assertEquals("new-rec-dir", videoRoomEditRequest.getNewRecDir());
  }

  @Test
  void test_equals_ok() {
    VideoRoomEditRequest videoRoomEditRequest =
        VideoRoomEditRequest.create()
            .request("edit")
            .room("video-room-id")
            .newRecDir("new-rec-dir");

    assertEquals(
        VideoRoomEditRequest.create()
            .request("edit")
            .room("video-room-id")
            .newRecDir("new-rec-dir"),
        videoRoomEditRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomEditRequest videoRoomEditRequest =
        VideoRoomEditRequest.create().request("edit").room("video-room-id");

    assertNotEquals(
        VideoRoomEditRequest.create().request("edit").room("room-id"), videoRoomEditRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomEditRequest videoRoomEditRequest =
        VideoRoomEditRequest.create().request("edit").room("video-room-id");

    assertNotEquals(null, videoRoomEditRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomEditRequest videoRoomEditRequest =
        VideoRoomEditRequest.create().request("edit").room("video-room-id");

    assertEquals(
        VideoRoomEditRequest.create().request("edit").room("video-room-id").hashCode(),
        videoRoomEditRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomEditRequest videoRoomEditRequest =
        VideoRoomEditRequest.create().request("edit").room("video-room-id");

    assertNotEquals(
        VideoRoomEditRequest.create().request("edit").room("room-id").hashCode(),
        videoRoomEditRequest.hashCode());
  }
}
