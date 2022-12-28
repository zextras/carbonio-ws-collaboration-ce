// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a successful response provided by VideoServer when creating a new 'connection' (session)
 * <p>
 * It's composed of:
 * <ul>
 *   <li>janus: "success"</li>
 *   <li>transaction: the transaction identifier related to the request previously sent</li>
 *   <li>data: a JSON object containing one field</li>
 *     <ul>
 *       <li>id: the unique session identifier</li>
 *     </ul>
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Data {

    private String id;

    public String getId() {
      return id;
    }
  }
}
