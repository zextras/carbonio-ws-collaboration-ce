package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonInclude(Include.NON_NULL)
public class JanusSessionInteractionRequest extends JanusSessionCreationRequest {

  @JsonProperty("plugin")
  private String pluginName;

  public JanusSessionInteractionRequest() {
  }

  public JanusSessionInteractionRequest(
    String action,
    UUID transactionId,
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

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return this.toString();
    }
  }
}
