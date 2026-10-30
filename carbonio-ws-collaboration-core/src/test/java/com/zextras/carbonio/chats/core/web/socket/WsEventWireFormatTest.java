// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.async.model.ReactionChanged;
import com.zextras.carbonio.async.model.Typing;
import org.junit.jupiter.api.Test;

class WsEventWireFormatTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void reactionChangedOperationKeepsItsLowerCaseWireValue() throws Exception {
    String json =
        objectMapper.writeValueAsString(
            ReactionChanged.create()
                .messageId("message-id")
                .roomId("room-id")
                .userId("user-id")
                .reaction("x")
                .operation(ReactionChanged.OperationEnum.REMOVED));

    assertTrue(json.contains("\"operation\":\"removed\""), json);
  }

  @Test
  void typingStatusKeepsItsLowerCaseWireValue() throws Exception {
    String json =
        objectMapper.writeValueAsString(
            Typing.create().roomId("room-id").userId("user-id").status(Typing.StatusEnum.STOPPED));

    assertTrue(json.contains("\"status\":\"stopped\""), json);
  }
}
