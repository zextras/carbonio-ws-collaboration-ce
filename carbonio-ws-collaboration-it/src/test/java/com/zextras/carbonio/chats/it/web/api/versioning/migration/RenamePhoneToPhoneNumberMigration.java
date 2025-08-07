// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api.versioning.migration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.core.web.api.versioning.ApiVersionMigration;
import com.zextras.carbonio.chats.it.web.api.versioning.DummyModel;

public class RenamePhoneToPhoneNumberMigration implements ApiVersionMigration {

  @Override
  public boolean canDowngrade(Class<?> clazz) {
    return clazz == DummyModel.class;
  }

  @Override
  public ObjectNode downgrade(ObjectNode input) {
    input.put("phone", input.get("phoneNumber").asText());
    input.remove("phoneNumber");
    return input;
  }
}
