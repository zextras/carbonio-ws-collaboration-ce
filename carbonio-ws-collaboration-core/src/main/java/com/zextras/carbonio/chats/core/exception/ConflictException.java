// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import jakarta.ws.rs.core.Response.Status;
import java.io.Serializable;

public class ConflictException extends ChatsHttpException implements Serializable {

  private static final Status HTTP_STATUS = Status.CONFLICT;
  private static final boolean IS_TO_LOG = false;
  private static final long serialVersionUID = -755505103440233196L;

  public ConflictException() {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase());
  }

  public ConflictException(Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), cause);
  }

  public ConflictException(String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo);
  }

  public ConflictException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo, cause);
  }

  public ConflictException(String error, String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo);
  }

  public ConflictException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo, cause);
  }

  protected ConflictException(
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
