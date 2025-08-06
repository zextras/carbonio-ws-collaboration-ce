// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import java.util.Map;

public class ChangeEvenTypeNameMigration implements ApiVersionMigration {

  private final Map<String, String> eventyTypeMap =
      Map.of(
          "WebsocketConnected", "websocketConnected",
          "Ping", "ping",
          "Pong", "pong");

  @Override
  public boolean canDowngrade(Class<?> clazz) {
    return DomainEvent.class.isAssignableFrom(clazz);
  }

  @Override
  public ObjectNode downgrade(ObjectNode input) {
    String type = input.get("type").textValue();
    input.remove("type");

    if (eventyTypeMap.containsKey(type)) {
      input.put("type", eventyTypeMap.get(type));
      return input;
    }

    input.put("type", EventType.fromString(StringFormatUtils.toConstantCase(type)).name());
    return input;
  }
}
