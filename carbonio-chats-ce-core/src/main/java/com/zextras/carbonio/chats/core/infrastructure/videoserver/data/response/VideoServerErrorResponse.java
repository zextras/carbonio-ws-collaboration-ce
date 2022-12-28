// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents an error response provided by VideoServer
 * <p>
 * It's composed of:
 * <ul>
 *   <li>janus: "error"</li>
 *   <li>transaction: the transaction identifier of the request that failed</li>
 *   <li>error: a JSON object containing two fields</li>
 *     <ul>
 *       <li>code: a numeric error code</li>
 *       <li>reason: a verbose string describing the cause of the failure</li>
 *     </ul>
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
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
