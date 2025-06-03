// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.core.provider.impl.ObjectMapperProvider;
import java.util.List;

public class ApiVersionMigrator {

  private final ObjectMapper mapper = ObjectMapperProvider.getObjectMapper();
  private final List<ApiVersionMigration> migrations;

  public ApiVersionMigrator(List<ApiVersionMigration> migrations) {
    this.migrations = migrations;
  }

  public Object downgrade(Object original) {
    ObjectNode jsonNode = mapper.valueToTree(original);
    return migrations.stream()
        .reduce(jsonNode, (response, migration) -> migration.downgrade(response), (a, b) -> b);
  }
}
