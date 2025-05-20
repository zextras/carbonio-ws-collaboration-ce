// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.utility;

import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpClientProvider {

  private static final int MAX_TOTAL_CONNECTIONS = 250;
  private static final int MAX_CONNECTIONS_PER_ROUTE = 50;
  private static final int KEEP_ALIVE_SECONDS = 30;
  private static final int TIMEOUT_MILLIS = 5000;

  private static final PoolingHttpClientConnectionManager connectionManager;
  private static final CloseableHttpClient httpClient;
  private static final ScheduledExecutorService executor;

  private HttpClientProvider() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(HttpClientProvider::shutdown, "Http client provider shutdown hook"));
  }

  static {
    connectionManager = new PoolingHttpClientConnectionManager(30, TimeUnit.SECONDS);
    connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
    connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

    executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(
        () -> {
          connectionManager.closeExpiredConnections();
          connectionManager.closeIdleConnections(KEEP_ALIVE_SECONDS, TimeUnit.SECONDS);
        },
        10,
        10,
        TimeUnit.SECONDS);

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(TIMEOUT_MILLIS)
            .setSocketTimeout(TIMEOUT_MILLIS)
            .setConnectionRequestTimeout(TIMEOUT_MILLIS)
            .build();

    httpClient =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setKeepAliveStrategy((response, context) -> KEEP_ALIVE_SECONDS * 1000L)
            .setDefaultRequestConfig(requestConfig)
            .build();
  }

  public static CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  public static void shutdown() {
    try {
      ChatsLogger.info("Shutting down HttpClientProvider...");
      httpClient.close();
      connectionManager.shutdown();
      executor.shutdown();
    } catch (IOException e) {
      throw new InternalErrorException(e);
    }
  }
}
