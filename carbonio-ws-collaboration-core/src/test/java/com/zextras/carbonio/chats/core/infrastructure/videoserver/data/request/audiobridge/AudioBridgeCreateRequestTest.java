// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
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
            .secret("secret")
            .pin("1234")
            .isPrivate(false)
            .allowed(List.of("user-id"))
            .samplingRate(1234L)
            .spatialAudio(false)
            .audioLevelExt(false)
            .audioLevelEvent(true)
            .audioActivePackets(4321L)
            .audioLevelAverage(10)
            .defaultPreBuffering(0L)
            .defaultExpectedLoss(0)
            .defaultBitrate(65000)
            .record(false)
            .recordFile("rec-file")
            .recordDir("rec-dir")
            .mjrs(false)
            .mjrsDir("mjrs-dir")
            .allowRtpParticipants(false)
            .groups(List.of("group"));

    assertEquals("create", audioBridgeCreateRequest.getRequest());
    assertEquals("audio-room-id", audioBridgeCreateRequest.getRoom());
    assertFalse(audioBridgeCreateRequest.getPermanent());
    assertEquals("description", audioBridgeCreateRequest.getDescription());
    assertEquals("secret", audioBridgeCreateRequest.getSecret());
    assertEquals("1234", audioBridgeCreateRequest.getPin());
    assertFalse(audioBridgeCreateRequest.getIsPrivate());
    assertEquals(List.of("user-id"), audioBridgeCreateRequest.getAllowed());
    assertEquals(1234L, audioBridgeCreateRequest.getSamplingRate());
    assertFalse(audioBridgeCreateRequest.getSpatialAudio());
    assertFalse(audioBridgeCreateRequest.getAudioLevelExt());
    assertTrue(audioBridgeCreateRequest.getAudioLevelEvent());
    assertEquals(4321L, audioBridgeCreateRequest.getAudioActivePackets());
    assertEquals(10, audioBridgeCreateRequest.getAudioLevelAverage());
    assertEquals(0L, audioBridgeCreateRequest.getDefaultPreBuffering());
    assertEquals(0, audioBridgeCreateRequest.getDefaultExpectedLoss());
    assertEquals(65000, audioBridgeCreateRequest.getDefaultBitrate());
    assertFalse(audioBridgeCreateRequest.getRecord());
    assertEquals("rec-file", audioBridgeCreateRequest.getRecordFile());
    assertEquals("rec-dir", audioBridgeCreateRequest.getRecordDir());
    assertFalse(audioBridgeCreateRequest.getMjrs());
    assertEquals("mjrs-dir", audioBridgeCreateRequest.getMjrsDir());
    assertFalse(audioBridgeCreateRequest.getAllowRtpParticipants());
    assertEquals(List.of("group"), audioBridgeCreateRequest.getGroups());
  }

  @Test
  void test_equals_ok() {
    AudioBridgeCreateRequest audioBridgeCreateRequest =
        AudioBridgeCreateRequest.create().request("create").room("audio-room-id");

    assertEquals(
        AudioBridgeCreateRequest.create().request("create").room("audio-room-id"),
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
