// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import com.zextras.carbonio.chats.core.provider.impl.ObjectMapperProvider;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class JacksonConfig
    implements ContextResolver<ObjectMapper> {

  private final ObjectMapper objectMapper;

  public JacksonConfig() {
    objectMapper =
        JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .addModule(new JavaTimeModule())
            .build()
            .setDateFormat(new RFC3339DateFormat())
            .setDefaultPropertyInclusion(Include.NON_NULL)
            .setSerializationInclusion(Include.NON_NULL);
    ObjectMapperProvider.setObjectMapper(objectMapper);
  }

  public ObjectMapper getContext(Class<?> arg0) {
    return objectMapper;
  }

  /** CE configures one ObjectMapper for REST and for every injection point (was a Guice Provider). */
  @Produces
  @Singleton
  public ObjectMapper get() {
    return objectMapper;
  }
}
