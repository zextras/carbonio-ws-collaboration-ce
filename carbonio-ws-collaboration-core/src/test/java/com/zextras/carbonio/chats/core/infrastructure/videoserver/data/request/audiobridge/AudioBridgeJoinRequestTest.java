// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioBridgeJoinRequestTest {

  @Test
  void test_builder_ok() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create()
            .request("join")
            .room("audio-room-id")
            .id("id")
            .muted(false)
            .filename("file-name");

    assertEquals("join", audioBridgeJoinRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeJoinRequest.getRoom());
    assertEquals("id", audioBridgeJoinRequest.getId());
    assertFalse(audioBridgeJoinRequest.getMuted());
    assertEquals("file-name", audioBridgeJoinRequest.getFilename());
  }

  @Test
  void test_equals_ok() {
    AudioBridgeJoinRequest audioBridgeJoinRequest =
        AudioBridgeJoinRequest.create()
            .request("join")
            .room("audio-room-id")
            .id("id")
            .muted(false)
            .filename("file-name");

    assertEquals(
        AudioBridgeJoinRequest.create()
            .request("join")
            .room("audio-room-id")
            .id("id")
            .muted(false)
            .filename("file-name"),
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
