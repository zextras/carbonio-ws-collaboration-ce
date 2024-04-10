// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRoomCreateRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create()
            .request("create")
            .room("video-room-id")
            .permanent(false)
            .description("description")
            .isPrivate(false)
            .publishers(100)
            .bitrate(65000L)
            .bitrateCap(true)
            .videoCodec("vp8")
            .record(false)
            .recDir("rec-dir");

    assertEquals("create", videoRoomCreateRequest.getRequest());
    assertEquals("video-room-id", videoRoomCreateRequest.getRoom());
    assertFalse(videoRoomCreateRequest.getPermanent());
    assertEquals("description", videoRoomCreateRequest.getDescription());
    assertFalse(videoRoomCreateRequest.getIsPrivate());
    assertEquals(100, videoRoomCreateRequest.getPublishers());
    assertEquals(65000L, videoRoomCreateRequest.getBitrate());
    assertTrue(videoRoomCreateRequest.getBitrateCap());
    assertEquals("vp8", videoRoomCreateRequest.getVideoCodec());
    assertFalse(videoRoomCreateRequest.getRecord());
    assertEquals("rec-dir", videoRoomCreateRequest.getRecDir());
  }

  @Test
  void test_equals_ok() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create()
            .request("create")
            .room("video-room-id")
            .permanent(false)
            .description("description")
            .isPrivate(false)
            .publishers(100)
            .bitrate(65000L)
            .bitrateCap(true)
            .videoCodec("vp8")
            .record(false)
            .recDir("rec-dir");

    assertEquals(
        VideoRoomCreateRequest.create()
            .request("create")
            .room("video-room-id")
            .permanent(false)
            .description("description")
            .isPrivate(false)
            .publishers(100)
            .bitrate(65000L)
            .bitrateCap(true)
            .videoCodec("vp8")
            .record(false)
            .recDir("rec-dir"),
        videoRoomCreateRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertNotEquals(
        VideoRoomCreateRequest.create().request("create").room("room-id"), videoRoomCreateRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertNotEquals(null, videoRoomCreateRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertEquals(
        VideoRoomCreateRequest.create().request("create").room("video-room-id").hashCode(),
        videoRoomCreateRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertNotEquals(
        VideoRoomCreateRequest.create().request("create").room("room-id").hashCode(),
        videoRoomCreateRequest.hashCode());
  }
}
