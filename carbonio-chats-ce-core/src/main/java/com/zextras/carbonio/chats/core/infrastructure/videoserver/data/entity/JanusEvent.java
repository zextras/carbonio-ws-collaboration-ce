package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JanusEvent {

  @JsonProperty("janus")
  private String     type;
  private long       sender;
  @JsonProperty("transaction")
  private String     transactionId;
  private PluginData pluginData;

  public JanusEvent() {
  }

  public JanusEvent(
    String type,
    long sender,
    String transactionId,
    PluginData pluginData
  ) {
    this.type = type;
    this.sender = sender;
    this.transactionId = transactionId;
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

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public PluginData getPluginData() {
    return pluginData;
  }

  public void setPluginData(PluginData pluginData) {
    this.pluginData = pluginData;
  }

  private static class PluginData {

    private String plugin;
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
}
