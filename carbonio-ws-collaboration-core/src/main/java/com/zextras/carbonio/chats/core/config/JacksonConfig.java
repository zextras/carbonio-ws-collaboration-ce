// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class JacksonConfig implements javax.inject.Provider<ObjectMapper>, ContextResolver<ObjectMapper> {

  private final ObjectMapper objectMapper;

  public JacksonConfig() {
    objectMapper = JsonMapper.builder()
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
      .build()
      .registerModule(new JavaTimeModule())
      .setDateFormat(new RFC3339DateFormat())
      .setDefaultPropertyInclusion(Include.NON_NULL);
  }

  public ObjectMapper getContext(Class<?> arg0) {
    return objectMapper;
  }

  @Override
  public ObjectMapper get() {
    return objectMapper;
  }
}