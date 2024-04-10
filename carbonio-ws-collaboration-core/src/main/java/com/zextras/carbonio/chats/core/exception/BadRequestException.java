// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import jakarta.ws.rs.core.Response.Status;
import java.io.Serializable;

public class BadRequestException extends ChatsHttpException implements Serializable {

  private static final long serialVersionUID = 9186731066864545518L;
  private static final Status HTTP_STATUS = Status.BAD_REQUEST;
  private static final boolean IS_TO_LOG = false;

  public BadRequestException() {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase());
  }

  public BadRequestException(Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), cause);
  }

  public BadRequestException(String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo);
  }

  public BadRequestException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo, cause);
  }

  public BadRequestException(String error, String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo);
  }

  public BadRequestException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo, cause);
  }

  protected BadRequestException(
      String error,
      String debugInfo,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(
        HTTP_STATUS.getStatusCode(),
        HTTP_STATUS.getReasonPhrase(),
        error,
        debugInfo,
        cause,
        enableSuppression,
        writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}
