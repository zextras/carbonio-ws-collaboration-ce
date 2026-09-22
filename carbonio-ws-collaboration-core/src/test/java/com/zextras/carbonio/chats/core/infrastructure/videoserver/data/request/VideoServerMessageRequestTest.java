// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  @Test
  void test_serverId_getter_setter_roundtrip() {
    VideoServerMessageRequest request = VideoServerMessageRequest.create().serverId("srv-1");
    assertEquals("srv-1", request.getServerId());
  }

  @Test
  void test_equals_ignores_serverId() {
    VideoServerMessageRequest withServerId =
        VideoServerMessageRequest.create().messageRequest("message").serverId("srv-1");
    VideoServerMessageRequest withoutServerId =
        VideoServerMessageRequest.create().messageRequest("message");

    assertEquals(withoutServerId, withServerId);
    assertEquals(withoutServerId.hashCode(), withServerId.hashCode());
  }

  @Test
  void test_serverId_not_serialized_to_janus_wire() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    VideoServerMessageRequest request =
        VideoServerMessageRequest.create().messageRequest("message").serverId("srv-42");

    String json = objectMapper.writeValueAsString(request);

    assertFalse(json.contains("service_id"), "service_id must not appear in Janus JSON");
    assertFalse(json.contains("server_id"), "server_id must not appear in Janus JSON");
  }
}
