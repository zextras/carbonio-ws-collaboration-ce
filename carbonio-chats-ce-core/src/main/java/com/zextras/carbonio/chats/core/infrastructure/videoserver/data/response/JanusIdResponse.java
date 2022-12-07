package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JanusIdResponse extends JanusResponse {

  @JsonProperty("transaction")
  private String transactionId;
  private Data   data;

  public JanusIdResponse() {
  }

  public JanusIdResponse(
    String status,
    String transactionId,
    Data data
  ) {
    super(status);
    this.transactionId = transactionId;
    this.data = data;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public Data getData() {
    return data;
  }

  public void setData(Data data) {
    this.data = data;
  }

  public String getDataId() {
    return data.getId();
  }

  private static class Data {

    private String id;

    public Data() {
    }

    public Data(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }
}
