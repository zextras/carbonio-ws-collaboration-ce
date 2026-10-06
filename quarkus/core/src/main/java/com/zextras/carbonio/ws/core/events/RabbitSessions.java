// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.events;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

/** One auto-delete queue per websocket session, bound to the user's direct exchange. */
@ApplicationScoped
public class RabbitSessions {

  @Inject Connection connection;

  private final Map<String, Channel> channels = new ConcurrentHashMap<>();

  private static final Logger LOG = Logger.getLogger(RabbitSessions.class);

  public void setup(String userId, String queueId, Consumer<String> onMessage) {
    teardown(queueId);
    Channel channel = null;
    try {
      channel = connection.createChannel();
      channel.exchangeDeclare(userId, BuiltinExchangeType.DIRECT, false, false, null);
      channel.queueDeclare(queueId, false, false, true, null);
      channel.queueBind(queueId, userId, RabbitEventPublisher.USER_ROUTING_KEY);
      channel.basicConsume(
          queueId,
          true,
          (tag, delivery) -> {
            try {
              onMessage.accept(new String(delivery.getBody(), StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
              LOG.warnf(e, "Event consumer failed for queue '%s'", queueId);
            }
          },
          tag -> {});
      channels.put(queueId, channel);
    } catch (IOException | RuntimeException e) {
      closeQuietly(channel, queueId);
      if (e instanceof IOException io) {
        throw new UncheckedIOException("Unable to set up the event queue " + queueId, io);
      }
      throw (RuntimeException) e;
    }
  }

  public void teardown(String queueId) {
    Channel channel = channels.remove(queueId);
    if (channel == null) {
      return;
    }
    try {
      channel.queueDelete(queueId);
    } catch (Exception e) {
      LOG.debugf(e, "Unable to delete queue '%s' (auto-delete, harmless)", queueId);
    }
    closeQuietly(channel, queueId);
  }

  private static void closeQuietly(Channel channel, String queueId) {
    if (channel == null) {
      return;
    }
    try {
      if (channel.isOpen()) {
        channel.close();
      }
    } catch (Exception e) {
      LOG.debugf(e, "Unable to close channel of queue '%s'", queueId);
    }
  }
}
