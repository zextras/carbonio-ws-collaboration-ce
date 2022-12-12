package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoServerIdResponse extends VideoServerResponse {

  @JsonProperty("transaction")
  private String transactionId;
  private Data   data;

  public String getTransactionId() {
    return transactionId;
  }

  public Data getData() {
    return data;
  }

  public String getDataId() {
    return data.getId();
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Data {

    private String id;

    public String getId() {
      return id;
    }
  }
}
