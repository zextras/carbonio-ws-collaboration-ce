// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class AudioBridgeJoinRequestTest {

  @Test
  void test_builder_ok() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create()
            .request("join")
            .room("audio-room-id")
            .id("id")
            .group("group")
            .pin("1234")
            .display("display")
            .token("token")
            .muted(false)
            .codec(List.of("vp8"))
            .preBuffer(1234L)
            .bitrate(65000L)
            .quality(1)
            .expectedLoss(0)
            .volume(2)
            .spatialPosition(3)
            .secret("secret")
            .audioLevelAverage(50)
            .audioActivePackets(45L)
            .record(false)
            .filename("file-name");

    assertEquals("join", audioBridgeJoinRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeJoinRequest.getRoom());
    assertEquals("id", audioBridgeJoinRequest.getId());
    assertEquals("group", audioBridgeJoinRequest.getGroup());
    assertEquals("1234", audioBridgeJoinRequest.getPin());
    assertEquals("display", audioBridgeJoinRequest.getDisplay());
    assertEquals("token", audioBridgeJoinRequest.getToken());
    assertFalse(audioBridgeJoinRequest.getMuted());
    assertEquals(List.of("vp8"), audioBridgeJoinRequest.getCodec());
    assertEquals(1234L, audioBridgeJoinRequest.getPreBuffer());
    assertEquals(65000L, audioBridgeJoinRequest.getBitrate());
    assertEquals(1, audioBridgeJoinRequest.getQuality());
    assertEquals(0, audioBridgeJoinRequest.getExpectedLoss());
    assertEquals(2, audioBridgeJoinRequest.getVolume());
    assertEquals(3, audioBridgeJoinRequest.getSpatialPosition());
    assertEquals("secret", audioBridgeJoinRequest.getSecret());
    assertEquals(50, audioBridgeJoinRequest.getAudioLevelAverage());
    assertEquals(45L, audioBridgeJoinRequest.getAudioActivePackets());
    assertFalse(audioBridgeJoinRequest.getRecord());
    assertEquals("file-name", audioBridgeJoinRequest.getFilename());
  }

  @Test
  void test_equals_ok() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create().request("join").room("audio-room-id");

    assertEquals(
        AudioBridgeJoinRequest.create().request("join").room("audio-room-id"),
        audioBridgeJoinRequest);
  }

  @Test
  void test_equals_different_attributes() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create().request("join").room("audio-room-id");

    assertNotEquals(
        AudioBridgeJoinRequest.create().request("join").room("room-id"), audioBridgeJoinRequest);
  }

  @Test
  void test_equals_different_objects() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create().request("join").room("audio-room-id");

    assertNotEquals(null, audioBridgeJoinRequest);
  }

  @Test
  void test_hashCode_ok() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create().request("join").room("audio-room-id");

    assertEquals(
        AudioBridgeJoinRequest.create().request("join").room("audio-room-id").hashCode(),
        audioBridgeJoinRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create().request("join").room("audio-room-id");

    assertNotEquals(
        AudioBridgeJoinRequest.create().request("join").room("room-id").hashCode(),
        audioBridgeJoinRequest.hashCode());
  }
}
