// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Jsep;

/**
 * This class represents a single message/request/action sent to the VideoServer.
 * <p>
 * Its parameters are:
 * <ul>
 *   <li>janus: the message or the request value you want to send to the VideoServer</li>
 *   <li>transaction: a random string as transaction identifier</li>
 *   <li>plugin: (optional) the plugin name you want to interact with</li>
 *   <li>body: (optional) a request body containing info for the message/request/action needed</li>
 *   <li>apiSecret: (optional) the api secret is required is it's set on VideoServer configuration file</li>
 *   <li>jsep: (optional) SDP offer to negotiate a new PeerConnection or
 *   SDP answer to close the circle and complete the setup of the PeerConnection</li>
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoServerMessageRequest {

  @JsonProperty("janus")
  private String                   messageRequest;
  @JsonProperty("transaction")
  private String                   transactionId;
  @JsonProperty("plugin")
  private String                   pluginName;
  @JsonProperty("body")
  private VideoServerPluginRequest videoServerPluginRequest;
  @JsonProperty("apisecret")
  private String                   apiSecret;
  private Jsep                     jsep;

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

  public VideoServerMessageRequest videoServerPluginRequest(VideoServerPluginRequest videoServerPluginRequest) {
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

  public Jsep getJsep() {
    return jsep;
  }

  public VideoServerMessageRequest jsep(Jsep jsep) {
    this.jsep = jsep;
    return this;
  }
}
