// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcherClient;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlBody;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlResponse;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;

public class MessageDispatcherHttpClient implements MessageDispatcherClient {

  private static final String GRAPHQL_ENDPOINT = "/api/graphql";

  private final HttpClient httpClient;
  private final String baseUrl;
  private final String authToken;
  private final ObjectMapper objectMapper;

  public MessageDispatcherHttpClient(
      HttpClient httpClient, String baseUrl, String authToken, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.baseUrl = baseUrl;
    this.authToken = authToken;
    this.objectMapper = objectMapper;
  }

  @Override
  public GraphQlResponse executeQuery(String query) {
    try {
      String url =
          new URIBuilder(baseUrl + GRAPHQL_ENDPOINT)
              .addParameter("query", query)
              .build()
              .toString();
      try (CloseableHttpResponse response =
          httpClient.sendGet(url, Map.of("Authorization", "Basic " + authToken))) {
        return parseResponse(response, "GraphQL query");
      }
    } catch (URISyntaxException e) {
      throw new MessageDispatcherException("Unable to construct URI for GraphQL query", e);
    } catch (IOException e) {
      throw new MessageDispatcherException(
          "Error occurred executing MongooseIm GraphQL query: ", e);
    }
  }

  @Override
  public GraphQlResponse executeMutation(GraphQlBody body) {
    try (CloseableHttpResponse response =
        httpClient.sendPost(
            baseUrl + GRAPHQL_ENDPOINT,
            Map.of(
                "Authorization", "Basic " + authToken,
                "Accept", "application/json",
                "Content-Type", "application/json"),
            objectMapper.writeValueAsString(body))) {
      return parseResponse(response, "GraphQL mutation");
    } catch (IOException e) {
      throw new MessageDispatcherException(
          "Error occurred executing MongooseIm GraphQL mutation: ", e);
    }
  }

  private GraphQlResponse parseResponse(CloseableHttpResponse response, String context)
      throws IOException {
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      throw new MessageDispatcherException(
          "MongooseIm returns error on " + context + " request: " + statusCode);
    }
    GraphQlResponse graphQlResponse =
        objectMapper.readValue(
            IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
            GraphQlResponse.class);
    if (graphQlResponse.getErrors() != null) {
      throw new MessageDispatcherException(
          "MongooseIm GraphQL error on "
              + context
              + ": "
              + objectMapper.writeValueAsString(graphQlResponse.getErrors()));
    }
    return graphQlResponse;
  }
}
