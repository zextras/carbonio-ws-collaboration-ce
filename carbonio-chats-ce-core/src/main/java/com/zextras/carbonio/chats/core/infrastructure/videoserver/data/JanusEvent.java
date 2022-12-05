package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

public class JanusEvent {

  @JsonProperty("janus")
  private String     type;
  @JsonProperty("sender")
  private long       sender;
  @JsonProperty("transaction")
  private UUID       transaction;
  @JsonProperty("pluginData")
  private PluginData pluginData;

  public JanusEvent() {
  }

  public JanusEvent(
    String type,
    long sender,
    UUID transaction,
    PluginData pluginData
  ) {
    this.type = type;
    this.sender = sender;
    this.transaction = transaction;
    this.pluginData = pluginData;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public long getSender() {
    return sender;
  }

  public void setSender(long sender) {
    this.sender = sender;
  }

  public UUID getTransaction() {
    return transaction;
  }

  public void setTransaction(UUID transaction) {
    this.transaction = transaction;
  }

  public PluginData getPluginData() {
    return pluginData;
  }

  public void setPluginData(PluginData pluginData) {
    this.pluginData = pluginData;
  }

  private static class PluginData {

    @JsonProperty("plugin")
    private String plugin;
    @JsonProperty("data")
    private Data   data;

    public PluginData() {
    }

    public PluginData(String plugin, Data data) {
      this.plugin = plugin;
      this.data = data;
    }

    public String getPlugin() {
      return plugin;
    }

    public void setPlugin(String plugin) {
      this.plugin = plugin;
    }

    public Data getData() {
      return data;
    }

    public void setData(Data data) {
      this.data = data;
    }
  }

  private static class Data {

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
