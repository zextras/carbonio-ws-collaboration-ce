// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VideoRoomDescriptionTest {

  @Test
  void test_builder_ok() {
    VideoRoomDescription videoRoomDescription =
        VideoRoomDescription.create().mid("mid").description("description");

    assertEquals("mid", videoRoomDescription.getMid());
    assertEquals("description", videoRoomDescription.getDescription());
  }

  @Test
  void test_equals_ok() {
    VideoRoomDescription videoRoomDescription =
        VideoRoomDescription.create().mid("mid").description("description");

    assertEquals(
        VideoRoomDescription.create().mid("mid").description("description"), videoRoomDescription);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomDescription videoRoomDescription =
        VideoRoomDescription.create().mid("mid").description("description");

    assertNotEquals(
        VideoRoomDescription.create().mid("mid").description("description123"),
        videoRoomDescription);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomDescription videoRoomDescription =
        VideoRoomDescription.create().mid("mid").description("description");

    assertNotEquals(null, videoRoomDescription);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomDescription videoRoomDescription =
        VideoRoomDescription.create().mid("mid").description("description");

    assertEquals(
        VideoRoomDescription.create().mid("mid").description("description").hashCode(),
        videoRoomDescription.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomDescription videoRoomDescription =
        VideoRoomDescription.create().mid("mid").description("description");

    assertNotEquals(
        VideoRoomDescription.create().mid("mid").description("description123").hashCode(),
        videoRoomDescription.hashCode());
  }
}
