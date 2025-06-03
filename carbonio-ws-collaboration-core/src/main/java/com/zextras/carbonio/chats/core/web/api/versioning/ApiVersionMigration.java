// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ApiVersionMigration {

  boolean canDowngrade(Class<?> clazz);

  ObjectNode downgrade(ObjectNode input);
}
