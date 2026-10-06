// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.testsupport;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Stubs {

  private Stubs() {}

  public static void myself(
      WireMockServer wm, UUID userId, String type, String status, boolean wscFeature,
      Map<String, String> capabilities) {
    String caps =
        capabilities.entrySet().stream()
            .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
            .collect(Collectors.joining(","));
    String features = wscFeature ? "\"carbonioFeatureWscEnabled\"" : "";
    String json =
        """
        {"info":{"userId":"%s","email":"%s@test.local","fullName":"User %s","status":"%s","type":"%s"},
         "features":[%s],"capabilities":{%s},"locale":"en"}
        """
            .formatted(userId, userId, userId, status, type, features, caps);
    wm.stubFor(
        get(urlPathEqualTo("/internal/users/myself"))
            .withHeader("ZM_AUTH_TOKEN", equalTo(TestUsers.token(userId)))
            .willReturn(okJson(json)));
  }

  public static void activeUser(WireMockServer wm, UUID userId, Map<String, String> capabilities) {
    myself(wm, userId, "INTERNAL", "active", true, capabilities);
  }

  public static void activeUser(WireMockServer wm, UUID userId) {
    activeUser(wm, userId, Map.of());
  }

  public static void activeGuest(WireMockServer wm, UUID userId) {
    myself(wm, userId, "GUEST", "active", true, Map.of());
  }

  public static void janus(WireMockServer wm) {
    wm.stubFor(post(urlPathEqualTo("/janus"))
        .withRequestBody(matchingJsonPath("$.janus", equalTo("create")))
        .willReturn(okJson("{\"janus\":\"success\",\"data\":{\"id\":\"conn-1\"}}")));
    wm.stubFor(post(urlPathEqualTo("/janus/conn-1"))
        .withRequestBody(matchingJsonPath("$.plugin", equalTo("janus.plugin.audiobridge")))
        .willReturn(okJson("{\"janus\":\"success\",\"data\":{\"id\":\"h-audio\"}}")));
    wm.stubFor(post(urlPathEqualTo("/janus/conn-1"))
        .withRequestBody(matchingJsonPath("$.plugin", equalTo("janus.plugin.videoroom")))
        .willReturn(okJson("{\"janus\":\"success\",\"data\":{\"id\":\"h-video\"}}")));
    wm.stubFor(post(urlPathEqualTo("/janus/conn-1"))
        .withRequestBody(matchingJsonPath("$.janus", equalTo("destroy")))
        .willReturn(okJson("{\"janus\":\"success\"}")));
    pluginRoom(wm, "h-audio", "audiobridge", "audio-room");
    pluginRoom(wm, "h-video", "videoroom", "video-room");
  }

  private static void pluginRoom(WireMockServer wm, String handle, String plugin, String room) {
    String url = "/janus/conn-1/" + handle;
    wm.stubFor(post(urlPathEqualTo(url)).atPriority(10)
        .willReturn(okJson("{\"janus\":\"success\",\"plugindata\":{\"data\":{\"" + plugin + "\":\"success\"}}}")));
    wm.stubFor(post(urlPathEqualTo(url)).atPriority(1)
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("create")))
        .willReturn(okJson("{\"janus\":\"success\",\"plugindata\":{\"data\":{\"" + plugin
            + "\":\"created\",\"room\":\"" + room + "\"}}}")));
    wm.stubFor(post(urlPathEqualTo(url)).atPriority(1)
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("destroy")))
        .willReturn(okJson("{\"janus\":\"success\",\"plugindata\":{\"data\":{\"" + plugin + "\":\"destroyed\"}}}")));
  }
}
