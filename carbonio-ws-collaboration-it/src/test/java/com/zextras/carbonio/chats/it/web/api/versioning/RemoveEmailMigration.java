// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api.versioning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.core.web.api.versioning.ApiVersionMigration;

public class RemoveEmailMigration implements ApiVersionMigration {

  @Override
  public boolean canDowngrade(Class<?> clazz) {
    return clazz == DummyModel.class;
  }

  @Override
  public ObjectNode downgrade(ObjectNode input) {
    var firstName = input.get("firstName").asText().toLowerCase();
    var lastName = input.get("lastName").asText().toLowerCase();
    input.put("email", firstName + "." + lastName + "@example.com");
    return input;
  }
}
