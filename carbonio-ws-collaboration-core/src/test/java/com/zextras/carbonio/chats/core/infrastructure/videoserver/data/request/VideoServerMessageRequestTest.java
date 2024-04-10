// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoServerMessageRequestTest {

  @Test
  void test_equals_ok() {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create().messageRequest("message");

    assertEquals(
        VideoServerMessageRequest.create().messageRequest("message"), videoServerMessageRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create().messageRequest("message");

    assertNotEquals(
        VideoServerMessageRequest.create().messageRequest("attach"), videoServerMessageRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create().messageRequest("message");

    assertNotEquals(null, videoServerMessageRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create().messageRequest("message");

    assertEquals(
        VideoServerMessageRequest.create().messageRequest("message").hashCode(),
        videoServerMessageRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create().messageRequest("message");

    assertNotEquals(
        VideoServerMessageRequest.create().messageRequest("attach").hashCode(),
        videoServerMessageRequest.hashCode());
  }
}
