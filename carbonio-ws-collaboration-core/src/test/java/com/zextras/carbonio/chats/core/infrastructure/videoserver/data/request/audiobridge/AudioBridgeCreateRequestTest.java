// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioBridgeCreateRequestTest {

  @Test
  void test_builder_ok() {
    AudioBridgeCreateRequest audioBridgeCreateRequest =
        AudioBridgeCreateRequest.create()
            .request("create")
            .room("audio-room-id")
            .permanent(false)
            .description("description")
            .isPrivate(false)
            .samplingRate(1234L)
            .audioLevelEvent(true)
            .audioActivePackets(4321L)
            .audioLevelAverage(10)
            .record(false)
            .mjrsDir("mjrs-dir");

    assertEquals("create", audioBridgeCreateRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeCreateRequest.getRoom());
    assertFalse(audioBridgeCreateRequest.getPermanent());
    assertEquals("description", audioBridgeCreateRequest.getDescription());
    assertFalse(audioBridgeCreateRequest.getIsPrivate());
    assertEquals(1234L, audioBridgeCreateRequest.getSamplingRate());
    assertTrue(audioBridgeCreateRequest.getAudioLevelEvent());
    assertEquals(4321L, audioBridgeCreateRequest.getAudioActivePackets());
    assertEquals(10, audioBridgeCreateRequest.getAudioLevelAverage());
    assertFalse(audioBridgeCreateRequest.getRecord());
    assertEquals("mjrs-dir", audioBridgeCreateRequest.getMjrsDir());
  }

  @Test
  void test_equals_ok() {
    AudioBridgeCreateRequest audioBridgeCreateRequest =
        AudioBridgeCreateRequest.create()
            .request("create")
            .room("audio-room-id")
            .permanent(false)
            .description("description")
            .isPrivate(false)
            .samplingRate(1234L)
            .audioLevelEvent(true)
            .audioActivePackets(4321L)
            .audioLevelAverage(10)
            .record(false)
            .mjrsDir("mjrs-dir");

    assertEquals(
        AudioBridgeCreateRequest.create()
            .request("create")
            .room("audio-room-id")
            .permanent(false)
            .description("description")
            .isPrivate(false)
            .samplingRate(1234L)
            .audioLevelEvent(true)
            .audioActivePackets(4321L)
            .audioLevelAverage(10)
            .record(false)
            .mjrsDir("mjrs-dir"),
        audioBridgeCreateRequest);
  }

  @Test
  void test_equals_different_attributes() {
    AudioBridgeCreateRequest audioBridgeCreateRequest =
        AudioBridgeCreateRequest.create().request("create").room("audio-room-id");

    assertNotEquals(
        AudioBridgeCreateRequest.create().request("create").room("room-id"),
        audioBridgeCreateRequest);
  }

  @Test
  void test_equals_different_objects() {
    AudioBridgeCreateRequest audioBridgeCreateRequest =
        AudioBridgeCreateRequest.create().request("create").room("audio-room-id");

    assertNotEquals(null, audioBridgeCreateRequest);
  }

  @Test
  void test_hashCode_ok() {
    AudioBridgeCreateRequest audioBridgeCreateRequest =
        AudioBridgeCreateRequest.create().request("create").room("audio-room-id");

    assertEquals(
        AudioBridgeCreateRequest.create().request("create").room("audio-room-id").hashCode(),
        audioBridgeCreateRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    AudioBridgeCreateRequest audioBridgeCreateRequest =
        AudioBridgeCreateRequest.create().request("create").room("audio-room-id");

    assertNotEquals(
        AudioBridgeCreateRequest.create().request("create").room("room-id").hashCode(),
        audioBridgeCreateRequest.hashCode());
  }
}
