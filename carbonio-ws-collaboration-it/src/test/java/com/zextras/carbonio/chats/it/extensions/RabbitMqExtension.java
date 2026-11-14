// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import com.zextras.carbonio.chats.it.tools.RabbitMqBroker;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class RabbitMqExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

  private static final Namespace EXTENSION_NAMESPACE = Namespace.create(RabbitMqExtension.class);

  @Override
  public void beforeAll(ExtensionContext context) {
    broker(context);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (!ExtensionUtils.isNestedClass(context)) broker(context).reset();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
    return parameter.getParameter().getType() == RabbitMqBroker.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
    return broker(context);
  }

  private static RabbitMqBroker broker(ExtensionContext context) {
    return context
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .getOrComputeIfAbsent(
            RabbitMqBroker.class, key -> new RabbitMqBroker(), RabbitMqBroker.class);
  }
}
