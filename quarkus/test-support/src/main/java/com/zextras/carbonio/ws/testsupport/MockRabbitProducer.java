// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.testsupport;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/** Replaces core's {@code @DefaultBean} ConnectionFactory whenever test-support is on the classpath. */
@ApplicationScoped
public class MockRabbitProducer {

  @Produces
  @Singleton
  ConnectionFactory mockConnectionFactory() {
    return new MockConnectionFactory();
  }
}
