// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoRoomJoinRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomJoinRequest videoRoomJoinRequest =
        VideoRoomJoinRequest.create()
            .request("join")
            .ptype("publisher")
            .room("video-room-id")
            .id("id")
            .display("display")
            .token("token")
            .useMsid(true)
            .autoupdate(true)
            .privateId("private-id")
            .streams(List.of(Stream.create().mid("mid")));

    assertEquals("join", videoRoomJoinRequest.getRequest());
    assertEquals("publisher", videoRoomJoinRequest.getPtype());
    assertEquals("video-room-id", videoRoomJoinRequest.getRoom());
    assertEquals("id", videoRoomJoinRequest.getId());
    assertEquals("display", videoRoomJoinRequest.getDisplay());
    assertEquals("token", videoRoomJoinRequest.getToken());
    assertTrue(videoRoomJoinRequest.isUseMsid());
    assertTrue(videoRoomJoinRequest.isAutoupdate());
    assertEquals("private-id", videoRoomJoinRequest.getPrivateId());
    assertEquals(List.of(Stream.create().mid("mid")), videoRoomJoinRequest.getStreams());
  }

  @Test
  void test_equals_ok() {
    VideoRoomJoinRequest videoRoomJoinRequest =
        VideoRoomJoinRequest.create().request("join").room("video-room-id");

    assertEquals(
        VideoRoomJoinRequest.create().request("join").room("video-room-id"), videoRoomJoinRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomJoinRequest videoRoomJoinRequest =
        VideoRoomJoinRequest.create().request("join").room("video-room-id");

    assertNotEquals(
        VideoRoomJoinRequest.create().request("join").room("room-id"), videoRoomJoinRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomJoinRequest videoRoomJoinRequest =
        VideoRoomJoinRequest.create().request("join").room("video-room-id");

    assertNotEquals(null, videoRoomJoinRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomJoinRequest videoRoomJoinRequest =
        VideoRoomJoinRequest.create().request("join").room("video-room-id");

    assertEquals(
        VideoRoomJoinRequest.create().request("join").room("video-room-id").hashCode(),
        videoRoomJoinRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomJoinRequest videoRoomJoinRequest =
        VideoRoomJoinRequest.create().request("join").room("video-room-id");

    assertNotEquals(
        VideoRoomJoinRequest.create().request("join").room("room-id").hashCode(),
        videoRoomJoinRequest.hashCode());
  }
}
