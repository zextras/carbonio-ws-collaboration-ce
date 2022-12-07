package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;

@JsonInclude(Include.NON_NULL)
public class JanusConnectionInteractionRequest extends JanusConnectionCreationRequest {

  @JsonProperty("plugin")
  private String pluginName;

  public JanusConnectionInteractionRequest() {
  }

  public JanusConnectionInteractionRequest(
    String action,
    String transactionId,
    @Nullable String pluginName
  ) {
    super(action, transactionId);
    this.pluginName = pluginName;
  }

  public String getPluginName() {
    return pluginName;
  }

  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }
}
