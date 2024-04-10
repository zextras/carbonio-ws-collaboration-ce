// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRoomPublishRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").filename("file-name");

    assertEquals("join", videoRoomPublishRequest.getRequest());
    assertEquals("file-name", videoRoomPublishRequest.getFilename());
  }

  @Test
  void test_equals_ok() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").filename("file-name");

    assertEquals(
        VideoRoomPublishRequest.create().request("join").filename("file-name"),
        videoRoomPublishRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").filename("file-name");

    assertNotEquals(
        VideoRoomPublishRequest.create().request("join").filename("file-name123"),
        videoRoomPublishRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").filename("file-name");

    assertNotEquals(null, videoRoomPublishRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").filename("file-name");

    assertEquals(
        VideoRoomPublishRequest.create().request("join").filename("file-name").hashCode(),
        videoRoomPublishRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").filename("file-name");

    assertNotEquals(
        VideoRoomPublishRequest.create().request("join").filename("file-name123").hashCode(),
        videoRoomPublishRequest.hashCode());
  }
}
