// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRoomEnableRecordingRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
        VideoRoomEnableRecordingRequest.create()
            .request("enable_recording")
            .room("video-room-id")
            .record(true);

    assertEquals("enable_recording", videoRoomEnableRecordingRequest.getRequest());
    assertEquals("video-room-id", videoRoomEnableRecordingRequest.getRoom());
    assertTrue(videoRoomEnableRecordingRequest.getRecord());
  }

  @Test
  void test_equals_ok() {
    VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
        VideoRoomEnableRecordingRequest.create()
            .request("enable_recording")
            .room("video-room-id")
            .record(true);

    assertEquals(
        VideoRoomEnableRecordingRequest.create()
            .request("enable_recording")
            .room("video-room-id")
            .record(true),
        videoRoomEnableRecordingRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
        VideoRoomEnableRecordingRequest.create().request("enable_recording").room("video-room-id");

    assertNotEquals(
        VideoRoomEnableRecordingRequest.create().request("enable_recording").room("room-id"),
        videoRoomEnableRecordingRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
        VideoRoomEnableRecordingRequest.create().request("enable_recording").room("video-room-id");

    assertNotEquals(null, videoRoomEnableRecordingRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
        VideoRoomEnableRecordingRequest.create().request("enable_recording").room("video-room-id");

    assertEquals(
        VideoRoomEnableRecordingRequest.create()
            .request("enable_recording")
            .room("video-room-id")
            .hashCode(),
        videoRoomEnableRecordingRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
        VideoRoomEnableRecordingRequest.create().request("enable_recording").room("video-room-id");

    assertNotEquals(
        VideoRoomEnableRecordingRequest.create()
            .request("enable_recording")
            .room("room-id")
            .hashCode(),
        videoRoomEnableRecordingRequest.hashCode());
  }
}
