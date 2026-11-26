// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.awaitility.Awaitility.await;

import com.google.inject.Inject;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class UserEventQueues {

  private static final String EXCHANGE = "wsc.events";
  private static final String USERS_HEADER = "users";

  private final ConnectionFactory connectionFactory;
  private final EventDispatcher eventDispatcher;
  private final Map<String, String> userByQueue = new ConcurrentHashMap<>();
  private Channel channel;

  @Inject
  public UserEventQueues(ConnectionFactory connectionFactory, EventDispatcher eventDispatcher) {
    this.connectionFactory = connectionFactory;
    this.eventDispatcher = eventDispatcher;
  }

  public String queueFor(String userId) throws IOException, TimeoutException {
    await().atMost(Duration.ofSeconds(5)).until(eventDispatcher::isAlive);
    Channel current = channel();
    current.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT, true);
    String queue = current.queueDeclare().getQueue();
    current.queueBind(queue, EXCHANGE, "");
    userByQueue.put(queue, userId);
    return queue;
  }

  public GetResponse basicGet(String queue, boolean autoAck) throws IOException, TimeoutException {
    String userId = userByQueue.get(queue);
    GetResponse response;
    while ((response = channel().basicGet(queue, autoAck)) != null) {
      if (isFor(response, userId)) return response;
    }
    return null;
  }

  private static boolean isFor(GetResponse response, String userId) {
    Map<String, Object> headers = response.getProps().getHeaders();
    return headers != null
        && headers.get(USERS_HEADER) instanceof List<?> users
        && users.stream().anyMatch(user -> user.toString().equals(userId));
  }

  private synchronized Channel channel() throws IOException, TimeoutException {
    if (channel == null || !channel.isOpen()) {
      ConnectionFactory factory = connectionFactory.clone();
      // a recovered connection would declare its queues again after the broker reset of each class
      factory.setAutomaticRecoveryEnabled(false);
      factory.setTopologyRecoveryEnabled(false);
      channel = factory.newConnection("it-user-event-queues").createChannel();
    }
    return channel;
  }
}
