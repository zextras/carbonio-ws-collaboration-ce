package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

public class JanusMessage {

  @JsonProperty("janus")
  private String      message;
  @JsonProperty("transaction")
  private UUID        transactionId;
  @JsonProperty("body")
  private RoomRequest body;

  public JanusMessage() {
  }

  public JanusMessage(
    String message,
    UUID transactionId,
    RoomRequest body
  ) {
    this.message = message;
    this.transactionId = transactionId;
    this.body = body;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public UUID getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(UUID transactionId) {
    this.transactionId = transactionId;
  }

  public RoomRequest getBody() {
    return body;
  }

  public void setBody(RoomRequest body) {
    this.body = body;
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
