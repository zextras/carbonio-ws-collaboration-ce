package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

public class JanusErrorResponse extends JanusResponse {

  @JsonProperty("transaction")
  private UUID  transactionId;
  @JsonProperty("error")
  private Error error;

  public JanusErrorResponse() {
  }

  public JanusErrorResponse(
    String status,
    UUID transactionId,
    Error error
  ) {
    super(status);
    this.transactionId = transactionId;
    this.error = error;
  }

  public UUID getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(UUID transactionId) {
    this.transactionId = transactionId;
  }

  public Error getError() {
    return error;
  }

  public void setData(Error error) {
    this.error = error;
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return this.toString();
    }
  }

  private static class Error {

    @JsonProperty("code")
    private long   code;
    @JsonProperty("reason")
    private String reason;

    public Error() {
    }

    public Error(long code, String reason) {
      this.code = code;
      this.reason = reason;
    }

    public long getCode() {
      return code;
    }

    public void setCode(long code) {
      this.code = code;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }
}
