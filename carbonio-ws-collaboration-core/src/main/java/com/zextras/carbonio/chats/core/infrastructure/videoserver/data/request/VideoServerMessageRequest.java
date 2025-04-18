// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import java.util.Objects;

/**
 * This class represents a single message/request/action sent to the VideoServer.
 *
 * <p>Its parameters are:
 *
 * <ul>
 *   <li>janus: the message or the request value you want to send to the VideoServer
 *   <li>transaction: a random string as transaction identifier
 *   <li>plugin: (optional) the plugin name you want to interact with
 *   <li>body: (optional) a request body containing info for the message/request/action needed
 *   <li>apiSecret: (optional) the api secret is required is it's set on VideoServer configuration
 *       file
 *   <li>jsep: (optional) SDP offer to negotiate a new PeerConnection or SDP answer to close the
 *       circle and complete the setup of the PeerConnection
 *   <li>opaque_id: a string you can use to link useful info like user-id
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoServerMessageRequest {

  @JsonProperty("janus")
  private String messageRequest;

  @JsonProperty("transaction")
  private String transactionId;

  @JsonProperty("plugin")
  private String pluginName;

  @JsonProperty("body")
  private VideoServerPluginRequest videoServerPluginRequest;

  @JsonProperty("apisecret")
  private String apiSecret;

  @JsonProperty("jsep")
  private RtcSessionDescription rtcSessionDescription;

  private String opaqueId;

  public static VideoServerMessageRequest create() {
    return new VideoServerMessageRequest();
  }

  public String getMessageRequest() {
    return messageRequest;
  }

  public VideoServerMessageRequest messageRequest(String request) {
    this.messageRequest = request;
    return this;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public VideoServerMessageRequest transactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public String getPluginName() {
    return pluginName;
  }

  public VideoServerMessageRequest pluginName(String pluginName) {
    this.pluginName = pluginName;
    return this;
  }

  public VideoServerPluginRequest getVideoServerPluginRequest() {
    return videoServerPluginRequest;
  }

  public VideoServerMessageRequest videoServerPluginRequest(
      VideoServerPluginRequest videoServerPluginRequest) {
    this.videoServerPluginRequest = videoServerPluginRequest;
    return this;
  }

  public String getApiSecret() {
    return apiSecret;
  }

  public VideoServerMessageRequest apiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
    return this;
  }

  public RtcSessionDescription getRtcSessionDescription() {
    return rtcSessionDescription;
  }

  public VideoServerMessageRequest rtcSessionDescription(
      RtcSessionDescription rtcSessionDescription) {
    this.rtcSessionDescription = rtcSessionDescription;
    return this;
  }

  public String getOpaqueId() {
    return opaqueId;
  }

  public VideoServerMessageRequest opaqueId(String opaqueId) {
    this.opaqueId = opaqueId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof VideoServerMessageRequest that)) return false;
    return Objects.equals(getMessageRequest(), that.getMessageRequest())
        && Objects.equals(getPluginName(), that.getPluginName())
        && Objects.equals(getVideoServerPluginRequest(), that.getVideoServerPluginRequest())
        && Objects.equals(getApiSecret(), that.getApiSecret())
        && Objects.equals(getRtcSessionDescription(), that.getRtcSessionDescription())
        && Objects.equals(getOpaqueId(), that.getOpaqueId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getMessageRequest(),
        getPluginName(),
        getVideoServerPluginRequest(),
        getApiSecret(),
        getRtcSessionDescription(),
        getOpaqueId());
  }
}
