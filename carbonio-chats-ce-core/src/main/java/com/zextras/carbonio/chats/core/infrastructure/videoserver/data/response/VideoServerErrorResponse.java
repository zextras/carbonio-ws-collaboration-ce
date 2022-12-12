package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoServerErrorResponse extends VideoServerResponse {

  @JsonProperty("transaction")
  private String transactionId;
  private Error  error;

  public String getTransactionId() {
    return transactionId;
  }

  public Error getError() {
    return error;
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Error {

    private long   code;
    private String reason;

    public long getCode() {
      return code;
    }

    public String getReason() {
      return reason;
    }
  }
}
