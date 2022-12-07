package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JanusErrorResponse extends JanusResponse {

  @JsonProperty("transaction")
  private String transactionId;
  private Error  error;

  public JanusErrorResponse() {
  }

  public JanusErrorResponse(
    String status,
    String transactionId,
    Error error
  ) {
    super(status);
    this.transactionId = transactionId;
    this.error = error;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public Error getError() {
    return error;
  }

  public void setData(Error error) {
    this.error = error;
  }

  private static class Error {

    private long   code;
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
