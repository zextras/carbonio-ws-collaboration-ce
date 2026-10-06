// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.filter;

import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * The CE UI compares {@code Content-Type} strictly with {@code application/json}, as Jetty sent
 * it. Quarkus REST appends {@code ;charset=UTF-8} to JSON it negotiates itself (plugin endpoints
 * included); this strips that parameter on every response, just before the headers are written.
 */
@ApplicationScoped
public class JsonContentTypeFilter {

  static final String JSON = "application/json";

  void register(@Observes Filters filters) {
    filters.register(this::filter, 1000);
  }

  void filter(RoutingContext context) {
    context.addHeadersEndHandler(
        v -> {
          String type = context.response().headers().get(HttpHeaders.CONTENT_TYPE);
          if (type != null && isJsonUtf8(type)) {
            context.response().headers().set(HttpHeaders.CONTENT_TYPE, JSON);
          }
        });
    context.next();
  }

  static boolean isJsonUtf8(String type) {
    String normalized = type.replace(" ", "").toLowerCase();
    return normalized.equals(JSON + ";charset=utf-8");
  }
}
