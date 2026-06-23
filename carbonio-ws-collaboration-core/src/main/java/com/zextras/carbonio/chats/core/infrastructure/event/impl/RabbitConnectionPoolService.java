// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerVideoserverHealthMonitor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RabbitConnectionPoolService {

  private static final String USER_ROUTING_KEY = "user-events";

  record SessionTopology(
      String userId,
      String sessionId,
      Channel channel,
      String consumerTag,
      DeliverCallback deliverCallback,
      Consumer<String> notifyCallback) {}

  private final List<Connection> connectionPool;
  private final AtomicInteger roundRobin = new AtomicInteger(0);
  private final ConcurrentHashMap<String, SessionTopology> topologyMap = new ConcurrentHashMap<>();
  private final AtomicInteger recoveredCount = new AtomicInteger(0);
  private final AtomicBoolean brokerDownGate = new AtomicBoolean(false);
  private final MessageBrokerVideoserverHealthMonitor healthMonitor;

  public RabbitConnectionPoolService(
      ConnectionFactory factory,
      int poolSize,
      MessageBrokerVideoserverHealthMonitor healthMonitor) {
    this.healthMonitor = healthMonitor;
    this.connectionPool = new ArrayList<>(poolSize);
    for (int i = 0; i < poolSize; i++) {
      try {
        Connection conn = factory.newConnection();
        attachRecoveryListener(conn);
        connectionPool.add(conn);
      } catch (IOException | TimeoutException e) {
        throw new EventDispatcherException("Could not create connection in pool", e);
      }
    }
    Runtime.getRuntime()
        .addShutdownHook(new Thread(this::stop, "RabbitConnectionPoolService shutdown"));
  }

  public void setupSession(
      String userId,
      String sessionId,
      DeliverCallback deliverCallback,
      Consumer<String> notifyCallback)
      throws IOException {
    Connection conn = nextConnection();
    Channel channel = conn.createChannel();
    channel.exchangeDeclare(userId, BuiltinExchangeType.DIRECT, false, false, null);
    channel.queueDeclare(sessionId, false, false, true, null);
    channel.queueBind(sessionId, userId, USER_ROUTING_KEY);
    String tag = channel.basicConsume(sessionId, true, deliverCallback, t -> {});
    topologyMap.put(
        sessionId,
        new SessionTopology(userId, sessionId, channel, tag, deliverCallback, notifyCallback));
  }

  public void teardownSession(String sessionId) {
    SessionTopology topology = topologyMap.remove(sessionId);
    if (topology == null) return;
    Channel ch = topology.channel();
    try {
      ch.basicCancel(topology.consumerTag());
    } catch (Exception ignored) {
    }
    try {
      ch.queueUnbind(sessionId, topology.userId(), USER_ROUTING_KEY);
    } catch (Exception ignored) {
    }
    try {
      ch.queueDeleteNoWait(sessionId, false, false);
    } catch (Exception ignored) {
    }
    try {
      if (ch.isOpen()) ch.close();
    } catch (Exception ignored) {
    }
  }

  public void stop() {
    connectionPool.forEach(
        conn -> {
          try {
            if (conn.isOpen()) conn.close();
          } catch (Exception e) {
            ChatsLogger.error("Error closing pool connection", e);
          }
        });
  }

  private Connection nextConnection() {
    int idx = (roundRobin.getAndIncrement() & Integer.MAX_VALUE) % connectionPool.size();
    return connectionPool.get(idx);
  }

  private void attachRecoveryListener(Connection connection) {
    connection.addShutdownListener(
        cause -> {
          if (!cause.isInitiatedByApplication() && brokerDownGate.compareAndSet(false, true)) {
            ChatsLogger.warn("RabbitMQ connection lost (shutdown signal received)");
            recoveredCount.set(0);
            healthMonitor.notifyBrokerDown();
          }
        });

    if (!(connection instanceof Recoverable)) return;
    ((Recoverable) connection)
        .addRecoveryListener(
            new RecoveryListener() {
              @Override
              public void handleRecoveryStarted(Recoverable recoverable) {
                ChatsLogger.warn("RabbitMQ connection recovery started");
              }

              @Override
              public void handleRecovery(Recoverable recoverable) {
                ChatsLogger.warn("RabbitMQ connection recovery completed");
                topologyMap
                    .values()
                    .forEach(
                        t -> {
                          try {
                            Channel ch = connection.createChannel();
                            ch.exchangeDeclare(
                                t.userId(), BuiltinExchangeType.DIRECT, false, false, null);
                            ch.queueDeclare(t.sessionId(), false, false, true, null);
                            ch.queueBind(t.sessionId(), t.userId(), USER_ROUTING_KEY);
                            String newTag =
                                ch.basicConsume(
                                    t.sessionId(), true, t.deliverCallback(), tag -> {});
                            topologyMap.put(
                                t.sessionId(),
                                new SessionTopology(
                                    t.userId(),
                                    t.sessionId(),
                                    ch,
                                    newTag,
                                    t.deliverCallback(),
                                    t.notifyCallback()));
                          } catch (Exception e) {
                            ChatsLogger.error(
                                "Failed to recover session topology: " + t.sessionId(), e);
                          }
                        });
                if (recoveredCount.incrementAndGet() >= connectionPool.size()) {
                  brokerDownGate.set(false);
                  healthMonitor.notifyBrokerRecovered();
                }
              }
            });
  }
}
