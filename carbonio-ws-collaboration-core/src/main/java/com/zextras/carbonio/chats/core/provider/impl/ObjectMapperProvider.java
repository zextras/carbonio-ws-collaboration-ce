// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperProvider {
  private static ObjectMapper objectMapper;

  private ObjectMapperProvider() {}

  public static void setObjectMapper(ObjectMapper mapper) {
    objectMapper = mapper;
  }

  public static ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      throw new IllegalStateException("ObjectMapper not initialized");
    }
    return objectMapper;
  }
}
