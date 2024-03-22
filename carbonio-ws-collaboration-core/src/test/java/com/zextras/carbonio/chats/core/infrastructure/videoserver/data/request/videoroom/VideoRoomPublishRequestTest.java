// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class VideoRoomPublishRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create()
            .request("join")
            .audioCodec("audio-codec")
            .videoCodec("vp8")
            .bitrate(65000L)
            .record(false)
            .filename("file-name")
            .display("display")
            .audioLevelAverage(50)
            .audioActivePackets(10L)
            .descriptions(List.of(VideoRoomDescription.create().mid("mid")));

    assertEquals("join", videoRoomPublishRequest.getRequest());
    assertEquals("audio-codec", videoRoomPublishRequest.getAudioCodec());
    assertEquals("vp8", videoRoomPublishRequest.getVideoCodec());
    assertEquals(65000L, videoRoomPublishRequest.getBitrate());
    assertFalse(videoRoomPublishRequest.isRecord());
    assertEquals("file-name", videoRoomPublishRequest.getFilename());
    assertEquals("display", videoRoomPublishRequest.getDisplay());
    assertEquals(50, videoRoomPublishRequest.getAudioLevelAverage());
    assertEquals(10L, videoRoomPublishRequest.getAudioActivePackets());
    assertEquals(
        List.of(VideoRoomDescription.create().mid("mid")),
        videoRoomPublishRequest.getDescriptions());
  }

  @Test
  void test_equals_ok() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").display("name");

    assertEquals(
        VideoRoomPublishRequest.create().request("join").display("name"), videoRoomPublishRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").display("name");

    assertNotEquals(
        VideoRoomPublishRequest.create().request("join").display("name123"),
        videoRoomPublishRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").display("name");

    assertNotEquals(null, videoRoomPublishRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").display("name");

    assertEquals(
        VideoRoomPublishRequest.create().request("join").display("name").hashCode(),
        videoRoomPublishRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomPublishRequest videoRoomPublishRequest =
        VideoRoomPublishRequest.create().request("join").display("name");

    assertNotEquals(
        VideoRoomPublishRequest.create().request("join").display("name123").hashCode(),
        videoRoomPublishRequest.hashCode());
  }
}
