// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoRoomConfigureRequestTest {

  @Test
  void serializes_configure_with_substream_per_stream() throws Exception {
    VideoRoomConfigureRequest req =
        VideoRoomConfigureRequest.create()
            .request("configure")
            .streams(List.of(ConfigureStream.create().mid("0").substream(1).temporal(2)));
    String json = new ObjectMapper().writeValueAsString(req);

    JsonNode node = new ObjectMapper().readTree(json);
    assertEquals("configure", node.get("request").asText());
    assertEquals("0", node.get("streams").get(0).get("mid").asText());
    assertEquals(1, node.get("streams").get(0).get("substream").asInt());
    assertEquals(2, node.get("streams").get(0).get("temporal").asInt());
    assertFalse(node.has("restart"));
  }
}
