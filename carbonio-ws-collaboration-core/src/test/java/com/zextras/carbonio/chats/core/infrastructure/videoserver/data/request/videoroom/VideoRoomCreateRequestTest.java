// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class VideoRoomCreateRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create()
            .request("create")
            .room("video-room-id")
            .permanent(false)
            .description("description")
            .secret("secret")
            .pin("1234")
            .isPrivate(false)
            .allowed(List.of("user-id"))
            .requirePvtId(false)
            .signedTokens(false)
            .publishers(100)
            .bitrate(65000L)
            .bitrateCap(true)
            .firFreq(1)
            .audioCodec("audio-codec")
            .videoCodec("vp8")
            .vp9Profile("vp9-profile")
            .h264Profile("h264-profile")
            .opusFEC(false)
            .opusDtx(false)
            .audioLevelExt(false)
            .audioLevelEvent(false)
            .audioLevelAverage(50)
            .videoOrientExt(false)
            .playOutDelayExt(false)
            .transportWideCCExt(false)
            .record(false)
            .recordDir("rec-dir")
            .lockRecord(false)
            .notifyJoining(false)
            .requireE2ee(false)
            .dummyPublisher(false)
            .dummyStreams(List.of("dummy-id"));

    assertEquals("create", videoRoomCreateRequest.getRequest());
    assertEquals("video-room-id", videoRoomCreateRequest.getRoom());
    assertFalse(videoRoomCreateRequest.getPermanent());
    assertEquals("description", videoRoomCreateRequest.getDescription());
    assertEquals("secret", videoRoomCreateRequest.getSecret());
    assertEquals("1234", videoRoomCreateRequest.getPin());
    assertFalse(videoRoomCreateRequest.getIsPrivate());
    assertEquals(List.of("user-id"), videoRoomCreateRequest.getAllowed());
    assertFalse(videoRoomCreateRequest.getRequirePvtId());
    assertFalse(videoRoomCreateRequest.getSignedTokens());
    assertEquals(100, videoRoomCreateRequest.getPublishers());
    assertEquals(65000L, videoRoomCreateRequest.getBitrate());
    assertTrue(videoRoomCreateRequest.getBitrateCap());
    assertEquals(1, videoRoomCreateRequest.getFirFreq());
    assertEquals("audio-codec", videoRoomCreateRequest.getAudioCodec());
    assertEquals("vp8", videoRoomCreateRequest.getVideoCodec());
    assertEquals("vp9-profile", videoRoomCreateRequest.getVp9Profile());
    assertEquals("h264-profile", videoRoomCreateRequest.getH264Profile());
    assertFalse(videoRoomCreateRequest.getOpusFec());
    assertFalse(videoRoomCreateRequest.getOpusDtx());
    assertFalse(videoRoomCreateRequest.getAudioLevelExt());
    assertFalse(videoRoomCreateRequest.getAudioLevelEvent());
    assertEquals(50, videoRoomCreateRequest.getAudioLevelAverage());
    assertFalse(videoRoomCreateRequest.getVideoOrientExt());
    assertFalse(videoRoomCreateRequest.getPlayOutDelayExt());
    assertFalse(videoRoomCreateRequest.getTransportWideCcExt());
    assertFalse(videoRoomCreateRequest.getRecord());
    assertEquals("rec-dir", videoRoomCreateRequest.getRecordDir());
    assertFalse(videoRoomCreateRequest.getLockRecord());
    assertFalse(videoRoomCreateRequest.getNotifyJoining());
    assertFalse(videoRoomCreateRequest.getRequireE2ee());
    assertFalse(videoRoomCreateRequest.getDummyPublisher());
    assertEquals(List.of("dummy-id"), videoRoomCreateRequest.getDummyStreams());
  }

  @Test
  void test_equals_ok() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertEquals(
        VideoRoomCreateRequest.create().request("create").room("video-room-id"),
        videoRoomCreateRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertNotEquals(
        VideoRoomCreateRequest.create().request("create").room("room-id"), videoRoomCreateRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertNotEquals(null, videoRoomCreateRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertEquals(
        VideoRoomCreateRequest.create().request("create").room("video-room-id").hashCode(),
        videoRoomCreateRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomCreateRequest videoRoomCreateRequest =
        VideoRoomCreateRequest.create().request("create").room("video-room-id");

    assertNotEquals(
        VideoRoomCreateRequest.create().request("create").room("room-id").hashCode(),
        videoRoomCreateRequest.hashCode());
  }
}
