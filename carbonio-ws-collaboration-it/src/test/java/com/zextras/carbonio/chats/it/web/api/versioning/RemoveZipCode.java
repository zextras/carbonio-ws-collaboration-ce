// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api.versioning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.core.web.api.versioning.ApiVersionMigration;

public class RemoveZipCode implements ApiVersionMigration {
  @Override
  public boolean canDowngrade(Class<?> clazz) {
    return clazz == ExampleModel.class;
  }

  @Override
  public ObjectNode downgrade(ObjectNode input) {
    input.put("zipCode", "00000");
    return input;
  }
}
