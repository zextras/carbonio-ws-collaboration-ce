// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioBridgeEditRequestTest {

  @Test
  void test_builder_ok() {
    AudioBridgeEditRequest audioBridgeEditRequest =
        AudioBridgeEditRequest.create()
            .request("edit")
            .room("audio-room-id")
            .newMjrsDir("new-mjrs-dir");

    assertEquals("edit", audioBridgeEditRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeEditRequest.getRoom());
    assertEquals("new-mjrs-dir", audioBridgeEditRequest.getNewMjrsDir());
  }

  @Test
  void test_equals_ok() {
    AudioBridgeEditRequest audioBridgeEditRequest =
        AudioBridgeEditRequest.create()
            .request("edit")
            .room("audio-room-id")
            .newMjrsDir("new-mjrs-dir");

    assertEquals(
        AudioBridgeEditRequest.create()
            .request("edit")
            .room("audio-room-id")
            .newMjrsDir("new-mjrs-dir"),
        audioBridgeEditRequest);
  }

  @Test
  void test_equals_different_attributes() {
    AudioBridgeEditRequest audioBridgeEditRequest =
        AudioBridgeEditRequest.create().request("edit").room("audio-room-id");

    assertNotEquals(
        AudioBridgeEditRequest.create().request("edit").room("room-id"), audioBridgeEditRequest);
  }

  @Test
  void test_equals_different_objects() {
    AudioBridgeEditRequest audioBridgeEditRequest =
        AudioBridgeEditRequest.create().request("edit").room("audio-room-id");

    assertNotEquals(null, audioBridgeEditRequest);
  }

  @Test
  void test_hashCode_ok() {
    AudioBridgeEditRequest audioBridgeEditRequest =
        AudioBridgeEditRequest.create().request("edit").room("audio-room-id");

    assertEquals(
        AudioBridgeEditRequest.create().request("edit").room("audio-room-id").hashCode(),
        audioBridgeEditRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    AudioBridgeEditRequest audioBridgeEditRequest =
        AudioBridgeEditRequest.create().request("edit").room("audio-room-id");

    assertNotEquals(
        AudioBridgeEditRequest.create().request("edit").room("room-id").hashCode(),
        audioBridgeEditRequest.hashCode());
  }
}
