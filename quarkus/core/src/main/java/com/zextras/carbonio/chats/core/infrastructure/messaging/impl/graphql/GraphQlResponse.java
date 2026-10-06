// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQlResponse {

  private JsonNode data;
  private List<JsonNode> errors;

  public JsonNode getData() {
    return data;
  }

  public List<JsonNode> getErrors() {
    return errors;
  }
}
