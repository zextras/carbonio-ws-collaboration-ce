package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JanusConnectionCreationRequest {

  @JsonProperty("janus")
  private String request;
  @JsonProperty("transaction")
  private String transactionId;

  public JanusConnectionCreationRequest() {
  }

  public JanusConnectionCreationRequest(
    String request,
    String transactionId
  ) {
    this.request = request;
    this.transactionId = transactionId;
  }

  public String getRequest() {
    return request;
  }

  public void setRequest(String request) {
    this.request = request;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }
}
