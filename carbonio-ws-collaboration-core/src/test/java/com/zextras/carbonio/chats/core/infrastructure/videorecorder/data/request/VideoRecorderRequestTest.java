// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRecorderRequestTest {

  @Test
  void test_equals_ok() {
    VideoRecorderRequest videoRecorderRequest =
        VideoRecorderRequest.create().recordingName("rec-name");

    assertEquals(VideoRecorderRequest.create().recordingName("rec-name"), videoRecorderRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRecorderRequest videoRecorderRequest =
        VideoRecorderRequest.create().recordingName("rec-name");

    assertNotEquals(
        VideoRecorderRequest.create().recordingName("rec-name123"), videoRecorderRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRecorderRequest videoRecorderRequest =
        VideoRecorderRequest.create().recordingName("rec-name");

    assertNotEquals(null, videoRecorderRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRecorderRequest videoRecorderRequest =
        VideoRecorderRequest.create().recordingName("rec-name");

    assertEquals(
        VideoRecorderRequest.create().recordingName("rec-name").hashCode(),
        videoRecorderRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRecorderRequest videoRecorderRequest =
        VideoRecorderRequest.create().recordingName("rec-name");

    assertNotEquals(
        VideoRecorderRequest.create().recordingName("rec-name123").hashCode(),
        videoRecorderRequest.hashCode());
  }
}
