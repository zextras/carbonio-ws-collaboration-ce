// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.*;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoRoomUpdateSubscriptionsRequestTest {

  @Test
  void test_builder_ok() {
    VideoRoomUpdateSubscriptionsRequest videoRoomUpdateSubscriptionsRequest =
        VideoRoomUpdateSubscriptionsRequest.create()
            .request("update")
            .subscriptions(List.of(Stream.create().mid("mid-1")))
            .unsubscriptions(List.of(Stream.create().mid("mid-2")));

    assertEquals("update", videoRoomUpdateSubscriptionsRequest.getRequest());
    assertEquals(
        List.of(Stream.create().mid("mid-1")),
        videoRoomUpdateSubscriptionsRequest.getSubscriptions());
    assertEquals(
        List.of(Stream.create().mid("mid-2")),
        videoRoomUpdateSubscriptionsRequest.getUnsubscriptions());
  }

  @Test
  void test_equals_ok() {
    VideoRoomUpdateSubscriptionsRequest videoRoomUpdateSubscriptionsRequest =
        VideoRoomUpdateSubscriptionsRequest.create().request("update");

    assertEquals(
        VideoRoomUpdateSubscriptionsRequest.create().request("update"),
        videoRoomUpdateSubscriptionsRequest);
  }

  @Test
  void test_equals_different_attributes() {
    VideoRoomUpdateSubscriptionsRequest videoRoomUpdateSubscriptionsRequest =
        VideoRoomUpdateSubscriptionsRequest.create().request("update");

    assertNotEquals(
        VideoRoomUpdateSubscriptionsRequest.create().request("request"),
        videoRoomUpdateSubscriptionsRequest);
  }

  @Test
  void test_equals_different_objects() {
    VideoRoomUpdateSubscriptionsRequest videoRoomUpdateSubscriptionsRequest =
        VideoRoomUpdateSubscriptionsRequest.create().request("update");

    assertNotEquals(null, videoRoomUpdateSubscriptionsRequest);
  }

  @Test
  void test_hashCode_ok() {
    VideoRoomUpdateSubscriptionsRequest videoRoomUpdateSubscriptionsRequest =
        VideoRoomUpdateSubscriptionsRequest.create().request("update");

    assertEquals(
        VideoRoomUpdateSubscriptionsRequest.create().request("update").hashCode(),
        videoRoomUpdateSubscriptionsRequest.hashCode());
  }

  @Test
  void test_hashCode_different_attributes() {
    VideoRoomUpdateSubscriptionsRequest videoRoomUpdateSubscriptionsRequest =
        VideoRoomUpdateSubscriptionsRequest.create().request("update");

    assertNotEquals(
        VideoRoomUpdateSubscriptionsRequest.create().request("request").hashCode(),
        videoRoomUpdateSubscriptionsRequest.hashCode());
  }
}
