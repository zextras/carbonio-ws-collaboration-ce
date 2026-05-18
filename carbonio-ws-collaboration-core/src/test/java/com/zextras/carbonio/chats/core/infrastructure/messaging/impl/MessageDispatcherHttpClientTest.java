// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlBody;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlResponse;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MessageDispatcherHttpClientTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String BASE_URL = "http://localhost:8088";
  private static final String AUTH_TOKEN = "test-token";
  private static final String GRAPHQL_ENDPOINT = BASE_URL + "/api/graphql";

  private final HttpClient httpClient;
  private final MessageDispatcherHttpClient dispatcherHttpClient;

  public MessageDispatcherHttpClientTest() {
    this.httpClient = mock(HttpClient.class);
    this.dispatcherHttpClient =
        new MessageDispatcherHttpClient(httpClient, BASE_URL, AUTH_TOKEN, OBJECT_MAPPER);
  }

  private CloseableHttpResponse mockHttpResponse(int statusCode, String body) throws IOException {
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity entity = mock(HttpEntity.class);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(statusCode);
    when(response.getEntity()).thenReturn(entity);
    when(entity.getContent())
        .thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    return response;
  }

  @Nested
  @DisplayName("executeQuery tests")
  class ExecuteQueryTests {

    @Test
    @DisplayName("Returns GraphQlResponse with data on HTTP 200")
    void executeQuery_returnsResponseOnSuccess() throws IOException {
      CloseableHttpResponse response =
          mockHttpResponse(200, "{\"data\":{\"checkAuth\":{\"authStatus\":true}}}");
      when(httpClient.sendGet(anyString(), any())).thenReturn(response);

      GraphQlResponse result =
          assertDoesNotThrow(() -> dispatcherHttpClient.executeQuery("query checkAuth { ... }"));

      assertNotNull(result.getData());
    }

    @Test
    @DisplayName("Sends GET to the GraphQL endpoint with Authorization header")
    void executeQuery_sendsGetToCorrectUrl() throws IOException {
      CloseableHttpResponse response = mockHttpResponse(200, "{\"data\":{}}");
      when(httpClient.sendGet(anyString(), any())).thenReturn(response);

      dispatcherHttpClient.executeQuery("query checkAuth { checkAuth { authStatus } }");

      verify(httpClient)
          .sendGet(
              startsWith(GRAPHQL_ENDPOINT), eq(Map.of("Authorization", "Basic " + AUTH_TOKEN)));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException on non-200 HTTP status")
    void executeQuery_throwsOnHttpError() throws IOException {
      CloseableHttpResponse response = mockHttpResponse(500, "Internal Server Error");
      when(httpClient.sendGet(anyString(), any())).thenReturn(response);

      assertThrows(
          MessageDispatcherException.class,
          () -> dispatcherHttpClient.executeQuery("query checkAuth { ... }"));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when response contains GraphQL errors")
    void executeQuery_throwsOnGraphQlErrors() throws IOException {
      CloseableHttpResponse response =
          mockHttpResponse(200, "{\"errors\":[{\"message\":\"unauthorized\"}]}");
      when(httpClient.sendGet(anyString(), any())).thenReturn(response);

      MessageDispatcherException ex =
          assertThrows(
              MessageDispatcherException.class,
              () -> dispatcherHttpClient.executeQuery("query checkAuth { ... }"));

      assertTrue(ex.getMessage().contains("unauthorized"));
    }
  }

  @Nested
  @DisplayName("executeMutation tests")
  class ExecuteMutationTests {

    private GraphQlBody aBody() {
      return GraphQlBody.create("mutation foo { foo }", "foo", Map.of());
    }

    @Test
    @DisplayName("Returns GraphQlResponse with data on HTTP 200")
    void executeMutation_returnsResponseOnSuccess() throws IOException {
      CloseableHttpResponse response =
          mockHttpResponse(200, "{\"data\":{\"stanza\":{\"sendStanza\":{\"id\":\"s1\"}}}}");
      when(httpClient.sendPost(anyString(), any(), anyString())).thenReturn(response);

      GraphQlResponse result =
          assertDoesNotThrow(() -> dispatcherHttpClient.executeMutation(aBody()));

      assertNotNull(result.getData());
    }

    @Test
    @DisplayName("Sends POST to the GraphQL endpoint with correct headers")
    void executeMutation_sendsPostWithCorrectHeaders() throws IOException {
      CloseableHttpResponse response = mockHttpResponse(200, "{\"data\":{}}");
      when(httpClient.sendPost(anyString(), any(), anyString())).thenReturn(response);

      dispatcherHttpClient.executeMutation(aBody());

      verify(httpClient)
          .sendPost(
              eq(GRAPHQL_ENDPOINT),
              eq(
                  Map.of(
                      "Authorization", "Basic " + AUTH_TOKEN,
                      "Accept", "application/json",
                      "Content-Type", "application/json")),
              anyString());
    }

    @Test
    @DisplayName("Throws MessageDispatcherException on non-200 HTTP status")
    void executeMutation_throwsOnHttpError() throws IOException {
      CloseableHttpResponse response = mockHttpResponse(503, "Service Unavailable");
      when(httpClient.sendPost(anyString(), any(), anyString())).thenReturn(response);

      assertThrows(
          MessageDispatcherException.class, () -> dispatcherHttpClient.executeMutation(aBody()));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when response contains GraphQL errors")
    void executeMutation_throwsOnGraphQlErrors() throws IOException {
      CloseableHttpResponse response =
          mockHttpResponse(200, "{\"errors\":[{\"message\":\"stanza rejected\"}]}");
      when(httpClient.sendPost(anyString(), any(), anyString())).thenReturn(response);

      MessageDispatcherException ex =
          assertThrows(
              MessageDispatcherException.class,
              () -> dispatcherHttpClient.executeMutation(aBody()));

      assertTrue(ex.getMessage().contains("stanza rejected"));
    }
  }
}
