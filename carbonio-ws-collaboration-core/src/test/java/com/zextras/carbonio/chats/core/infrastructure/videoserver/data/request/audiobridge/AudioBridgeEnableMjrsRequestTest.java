// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioBridgeEnableMjrsRequestTest {

  @Test
  void test_builder_ok() {
    AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
        AudioBridgeEnableMjrsRequest.create()
            .request("enable_mjrs")
            .room("audio-room-id")
            .mjrs(true);

    assertEquals("enable_mjrs", audioBridgeEnableMjrsRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeEnableMjrsRequest.getRoom());
    assertTrue(audioBridgeEnableMjrsRequest.getMjrs());
  }

  @Test
  void test_equals_ok() {
    AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
        AudioBridgeEnableMjrsRequest.create()
            .request("enable_mjrs")
            .room("audio-room-id")
            .mjrs(true);

    assertEquals(
        AudioBridgeEnableMjrsRequest.create()
            .request("enable_mjrs")
            .room("audio-room-id")
            .mjrs(true),
        audioBridgeEnableMjrsRequest);
  }

  @Test
  void test_equals_different_attributes() {
    AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
        AudioBridgeEnableMjrsRequest.create().request("enable_mjrs").room("audio-room-id");

    assertNotEquals(
        AudioBridgeEnableMjrsRequest.create().request("enable_mjrs").room("room-id"),
        audioBridgeEnableMjrsRequest);
  }

  @Test
  void test_equals_different_objects() {
    AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
        AudioBridgeEnableMjrsRequest.create().request("enable_mjrs").room("audio-room-id");

    assertNotEquals(null, audioBridgeEnableMjrsRequest);
  }

  @Test
  void test_hashCode_ok() {
    AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
        AudioBridgeEnableMjrsRequest.create().request("enable_mjrs").room("audio-room-id");

    assertEquals(
        AudioBridgeEnableMjrsRequest.create()
            .request("enable_mjrs")
            .room("audio-room-id")
            .hashCode(),
        audioBridgeEnableMjrsRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
        AudioBridgeEnableMjrsRequest.create().request("enable_mjrs").room("audio-room-id");

    assertNotEquals(
        AudioBridgeEnableMjrsRequest.create().request("enable_mjrs").room("room-id").hashCode(),
        audioBridgeEnableMjrsRequest.hashCode());
  }
}
