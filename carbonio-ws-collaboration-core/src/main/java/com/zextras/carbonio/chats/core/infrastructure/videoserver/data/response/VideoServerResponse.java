// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents a response provided by VideoServer when interacting with session or plugin
 * <p>
 * The successful response is composed of:
 * <ul>
 *   <li>janus: "success"</li>
 *   <li>session_id: the session identifier related to the session previously created</li>
 *   <li>transaction: the transaction identifier related to the request previously sent</li>
 *   <li>data: a JSON object containing one field</li>
 *     <ul>
 *       <li>id: the unique session identifier</li>
 *     </ul>
 * </ul>
 * <p>
 * The error response is composed of:
 * <ul>
 *   <li>janus: "error"</li>
 *   <li>session_id: the session identifier of the failed request</li>
 *   <li>transaction: the transaction identifier of the failed request</li>
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoServerResponse {

  @JsonProperty("janus")
  private String              status;
  @JsonProperty("sessionId")
  private String              connectionId;
  @JsonProperty("transaction")
  private String              transactionId;
  private VideoServerDataInfo data;
  private VideoServerError    error;

  public static VideoServerResponse create() {
    return new VideoServerResponse();
  }

  public String getStatus() {
    return status;
  }

  public VideoServerResponse status(String status) {
    this.status = status;
    return this;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public VideoServerResponse connectionId(String connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public VideoServerResponse transactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public VideoServerDataInfo getData() {
    return data;
  }

  public VideoServerResponse data(VideoServerDataInfo data) {
    this.data = data;
    return this;
  }

  public VideoServerError getError() {
    return error;
  }

  public VideoServerResponse error(VideoServerError error) {
    this.error = error;
    return this;
  }

  @JsonIgnore
  public String getDataId() {
    return getData().getId();
  }
}
