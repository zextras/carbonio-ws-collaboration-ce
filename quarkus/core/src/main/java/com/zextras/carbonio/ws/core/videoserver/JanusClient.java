// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.videoserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/** Janus REST transport — same wire calls as CE's VideoServerHttpClient. */
@ApplicationScoped
public class JanusClient {

  static final Duration TIMEOUT = Duration.ofMillis(5000);

  @Inject VideoServerConfig config;
  @Inject ObjectMapper mapper;

  private final HttpClient http =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(TIMEOUT).build();

  public static boolean isSuccess(JsonNode response) {
    return "success".equals(response.path("janus").asText());
  }

  public static String pluginStatus(JsonNode response, String plugin) {
    return response.path("plugindata").path("data").path(plugin).asText(null);
  }

  public String createConnection(String serverId) {
    JsonNode response = post(serverId, "", envelope("create"));
    requireSuccess(response, "Error creating video server connection");
    return response.path("data").path("id").asText();
  }

  public String attach(String serverId, String connectionId, String plugin, String opaqueId) {
    ObjectNode request = envelope("attach");
    request.put("plugin", plugin);
    request.put("opaque_id", opaqueId);
    JsonNode response = post(serverId, "/" + connectionId, request);
    requireSuccess(response, "Error attaching to plugin " + plugin);
    return response.path("data").path("id").asText();
  }

  public JsonNode message(String serverId, String connectionId, String handleId, Object body) {
    ObjectNode request = envelope("message");
    request.set("body", mapper.valueToTree(body));
    return post(serverId, "/" + connectionId + "/" + handleId, request);
  }

  public JsonNode detach(String serverId, String connectionId, String handleId) {
    return post(serverId, "/" + connectionId + "/" + handleId, envelope("detach"));
  }

  public JsonNode destroy(String serverId, String connectionId) {
    return post(serverId, "/" + connectionId, envelope("destroy"));
  }

  private ObjectNode envelope(String janus) {
    ObjectNode node = mapper.createObjectNode();
    node.put("janus", janus);
    node.put("transaction", UUID.randomUUID().toString());
    node.put("apisecret", config.apiSecret());
    return node;
  }

  private static void requireSuccess(JsonNode response, String error) {
    if (!isSuccess(response)) {
      throw new VideoServerException(error);
    }
  }

  private JsonNode post(String serverId, String path, ObjectNode payload) {
    String url =
        config.url() + "/janus" + path
            + (serverId == null ? "" : "?service_id=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8));
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(TIMEOUT)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
              .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new VideoServerException("Video server returns error response: " + response.statusCode());
      }
      return mapper.readTree(response.body());
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new VideoServerException("Interrupted while calling the video server", e);
    }
  }
}
