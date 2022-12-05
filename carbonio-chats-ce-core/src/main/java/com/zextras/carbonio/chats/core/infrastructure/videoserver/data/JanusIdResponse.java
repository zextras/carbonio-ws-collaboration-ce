package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class JanusIdResponse extends JanusResponse {

  @JsonProperty("transaction")
  private UUID transactionId;
  @JsonProperty("data")
  private Data data;

  public JanusIdResponse() {
  }

  public JanusIdResponse(
    String status,
    UUID transactionId,
    Data data
  ) {
    super(status);
    this.transactionId = transactionId;
    this.data = data;
  }

  public UUID getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(UUID transactionId) {
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

    @JsonProperty("id")
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
