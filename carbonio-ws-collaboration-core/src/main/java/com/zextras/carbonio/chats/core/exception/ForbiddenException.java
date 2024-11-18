// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import jakarta.ws.rs.core.Response.Status;
import java.io.Serial;
import java.io.Serializable;

public class ForbiddenException extends ChatsHttpException implements Serializable {

  @Serial private static final long serialVersionUID = -115828843775102406L;
  private static final Status HTTP_STATUS = Status.FORBIDDEN;
  private static final boolean IS_TO_LOG = false;

  public ForbiddenException() {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase());
  }

  public ForbiddenException(Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), cause);
  }

  public ForbiddenException(String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo);
  }

  public ForbiddenException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo, cause);
  }

  public ForbiddenException(String error, String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo);
  }

  public ForbiddenException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo, cause);
  }

  protected ForbiddenException(
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
