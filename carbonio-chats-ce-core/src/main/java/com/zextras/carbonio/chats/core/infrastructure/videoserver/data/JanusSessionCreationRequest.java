package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

public class JanusSessionCreationRequest {

  @JsonProperty("janus")
  private String request;
  @JsonProperty("transaction")
  private UUID   transactionId;

  public JanusSessionCreationRequest() {
  }

  public JanusSessionCreationRequest(
    String request,
    UUID transactionId
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

  public UUID getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(UUID transactionId) {
    this.transactionId = transactionId;
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
