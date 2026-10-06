// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.ws.plugin.api.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RabbitEventPublisher implements EventPublisher {

  static final String USER_ROUTING_KEY = "user-events";
  private static final Logger LOG = Logger.getLogger(RabbitEventPublisher.class);

  @Inject Channel channel;
  @Inject ObjectMapper objectMapper;

  @Override
  public void sendToUsers(Collection<UUID> userIds, DomainEvent event) {
    byte[] body;
    try {
      body = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      LOG.errorf(e, "Unable to serialize event %s", event.getType());
      return;
    }
    // Channel is not thread-safe for concurrent publishes.
    synchronized (channel) {
      for (UUID userId : userIds) {
        String exchange = userId.toString();
        try {
          channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, false, false, null);
          channel.basicPublish(exchange, USER_ROUTING_KEY, null, body);
        } catch (Exception e) {
          LOG.warnf(e, "Unable to send event to user '%s'", exchange);
        }
      }
    }
  }
}
