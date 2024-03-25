// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioBridgeMuteRequestTest {

  @Test
  void test_builder_ok() {
    AudioBridgeMuteRequest audioBridgeMuteRequest =
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id").id("id");

    assertEquals("mute", audioBridgeMuteRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeMuteRequest.getRoom());
    assertEquals("id", audioBridgeMuteRequest.getId());
  }

  @Test
  void test_equals_all_attributes() {
    AudioBridgeMuteRequest audioBridgeMuteRequest =
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id").id("id");

    assertEquals(
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id").id("id"),
        audioBridgeMuteRequest);
  }

  @Test
  void test_equals_different_attributes() {
    AudioBridgeMuteRequest audioBridgeMuteRequest =
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id");

    assertNotEquals(
        AudioBridgeMuteRequest.create().request("mute").room("room-id"), audioBridgeMuteRequest);
  }

  @Test
  void test_equals_different_objects() {
    AudioBridgeMuteRequest audioBridgeMuteRequest =
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id");

    assertNotEquals(null, audioBridgeMuteRequest);
  }

  @Test
  void test_hashCode_ok() {
    AudioBridgeMuteRequest audioBridgeMuteRequest =
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id");

    assertEquals(
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id").hashCode(),
        audioBridgeMuteRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    AudioBridgeMuteRequest audioBridgeMuteRequest =
        AudioBridgeMuteRequest.create().request("mute").room("audio-room-id");

    assertNotEquals(
        AudioBridgeMuteRequest.create().request("mute").room("room-id").hashCode(),
        audioBridgeMuteRequest.hashCode());
  }
}
