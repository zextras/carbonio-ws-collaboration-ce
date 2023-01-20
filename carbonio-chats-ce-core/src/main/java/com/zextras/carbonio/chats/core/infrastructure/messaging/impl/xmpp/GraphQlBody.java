// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import java.util.Map;

public class GraphQlBody {

  private String              query;
  private String              operationName;
  private Map<String, String> variables;

  public GraphQlBody(String query, String operationName, Map<String, String> variables) {
    this.query = query;
    this.operationName = operationName;
    this.variables = variables;
  }

  public static GraphQlBody create(String query, String operationName, Map<String, String> variables) {
    return new GraphQlBody(query, operationName, variables);
  }

  public String getQuery() {
    return query;
  }

  public String getOperationName() {
    return operationName;
  }

  public Map<String, String> getVariables() {
    return variables;
  }
}
