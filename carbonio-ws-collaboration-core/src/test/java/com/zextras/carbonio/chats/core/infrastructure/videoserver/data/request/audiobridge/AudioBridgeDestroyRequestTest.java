// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioBridgeDestroyRequestTest {

  @Test
  void test_builder_ok() {
    AudioBridgeDestroyRequest audioBridgeDestroyRequest =
        AudioBridgeDestroyRequest.create()
            .request("destroy")
            .room("audio-room-id")
            .secret("secret")
            .permanent(false);

    assertEquals("destroy", audioBridgeDestroyRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeDestroyRequest.getRoom());
    assertEquals("secret", audioBridgeDestroyRequest.getSecret());
    assertFalse(audioBridgeDestroyRequest.getPermanent());
  }

  @Test
  void test_equals_ok() {
    AudioBridgeDestroyRequest audioBridgeDestroyRequest =
        AudioBridgeDestroyRequest.create().request("destroy").room("audio-room-id");

    assertEquals(
        AudioBridgeDestroyRequest.create().request("destroy").room("audio-room-id"),
        audioBridgeDestroyRequest);
  }

  @Test
  void test_equals_different_attributes() {
    AudioBridgeDestroyRequest audioBridgeDestroyRequest =
        AudioBridgeDestroyRequest.create().request("destroy").room("audio-room-id");

    assertNotEquals(
        AudioBridgeDestroyRequest.create().request("destroy").room("room-id"),
        audioBridgeDestroyRequest);
  }

  @Test
  void test_equals_different_objects() {
    AudioBridgeDestroyRequest audioBridgeDestroyRequest =
        AudioBridgeDestroyRequest.create().request("destroy").room("audio-room-id");

    assertNotEquals(null, audioBridgeDestroyRequest);
  }

  @Test
  void test_hashCode_ok() {
    AudioBridgeDestroyRequest audioBridgeDestroyRequest =
        AudioBridgeDestroyRequest.create().request("destroy").room("audio-room-id");

    assertEquals(
        AudioBridgeDestroyRequest.create().request("destroy").room("audio-room-id").hashCode(),
        audioBridgeDestroyRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    AudioBridgeDestroyRequest audioBridgeDestroyRequest =
        AudioBridgeDestroyRequest.create().request("destroy").room("audio-room-id");

    assertNotEquals(
        AudioBridgeDestroyRequest.create().request("destroy").room("room-id").hashCode(),
        audioBridgeDestroyRequest.hashCode());
  }
}
