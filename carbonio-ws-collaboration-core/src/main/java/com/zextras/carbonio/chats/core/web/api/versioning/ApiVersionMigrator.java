// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.core.provider.impl.ObjectMapperProvider;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class ApiVersionMigrator {

  private final ObjectMapper mapper = ObjectMapperProvider.getObjectMapper();
  private final List<ApiVersionMigration> migrations;

  public ApiVersionMigrator(List<ApiVersionMigration> migrations) {
    this.migrations = migrations;
  }

  public Object downgrade(Object original) {
    /** The Object can be either a single DTO or a List<DTO>. */
    JsonNode jsonNode = mapper.valueToTree(original);

    if (jsonNode.isArray()) {
      return migrateList(jsonNode);
    }
    return migrate((ObjectNode) jsonNode);
  }

  private List<ObjectNode> migrateList(JsonNode jsonNode) {
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(jsonNode.elements(), Spliterator.ORDERED), false)
        .map(j -> (ObjectNode) j)
        .map(this::migrate)
        .toList();
  }

  private ObjectNode migrate(ObjectNode jsonNode) {
    return migrations.stream()
        .reduce(jsonNode, (response, migration) -> migration.downgrade(response), (a, b) -> b);
  }
}
