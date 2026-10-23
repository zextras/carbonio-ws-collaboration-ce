// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;

@Provider
@Singleton
public class OffsetDateTimeParamConverterProvider implements ParamConverterProvider {

  @Inject
  public OffsetDateTimeParamConverterProvider() {}

  @Override
  @SuppressWarnings("unchecked")
  public <T> ParamConverter<T> getConverter(
      Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (!OffsetDateTime.class.equals(rawType)) {
      return null;
    }
    return (ParamConverter<T>)
        new ParamConverter<OffsetDateTime>() {
          @Override
          public OffsetDateTime fromString(String value) {
            if (value == null || value.isBlank()) {
              return null;
            }
            return OffsetDateTime.parse(value);
          }

          @Override
          public String toString(OffsetDateTime value) {
            return value == null ? null : value.toString();
          }
        };
  }
}
