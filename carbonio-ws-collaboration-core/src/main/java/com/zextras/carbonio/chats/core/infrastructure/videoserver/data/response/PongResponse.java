// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents a pong response provided by VideoServer when a ping request is sent
 * <p>
 * The successful response is composed of:
 * <ul>
 *   <li>janus: "pong"</li>
 *   <li>transaction: the transaction identifier related to the request previously sent</li>
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PongResponse {

  public static final String PONG = "pong";

  @JsonProperty("janus")
  private String status;
  @JsonProperty("transaction")
  private String transactionId;

  public static PongResponse create() {
    return new PongResponse();
  }

  public String getStatus() {
    return status;
  }

  public PongResponse status(String status) {
    this.status = status;
    return this;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public PongResponse transactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PongResponse)) {
      return false;
    }
    PongResponse that = (PongResponse) o;
    return Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getTransactionId(),
      that.getTransactionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStatus(), getTransactionId());
  }
}
