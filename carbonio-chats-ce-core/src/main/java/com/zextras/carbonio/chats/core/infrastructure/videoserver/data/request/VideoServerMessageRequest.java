package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;

/**
 * This class represents a single message/request/action sent to the VideoServer.
 * <p>
 * Its parameters are:
 * <ul>
 *   <li>janus: the message or the request value you want to send to the VideoServer</li>
 *   <li>transaction: a random string as transaction identifier</li>
 *   <li>plugin: (optional) the plugin name you want to interact with</li>
 *   <li>body: (optional) a request body containing info for the message/request/action needed</li>
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
@JsonInclude(Include.NON_NULL)
public class VideoServerMessageRequest {

  @JsonProperty("janus")
  private String      messageRequest;
  @JsonProperty("transaction")
  private String      transactionId;
  @JsonProperty("plugin")
  private String      pluginName;
  private RoomRequest body;

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

  public VideoServerMessageRequest pluginName(@Nullable String pluginName) {
    this.pluginName = pluginName;
    return this;
  }

  public RoomRequest getBody() {
    return body;
  }

  public VideoServerMessageRequest body(@Nullable RoomRequest body) {
    this.body = body;
    return this;
  }
}
