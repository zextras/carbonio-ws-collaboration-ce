// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import java.io.Serializable;
import javax.ws.rs.core.Response.Status;

public class BadRequestException extends ChatsHttpException implements Serializable {

  private static final long    serialVersionUID = 9186731066864545518L;
  private static final Status  HTTP_STATUS      = Status.BAD_REQUEST;
  private static final boolean IS_TO_LOG        = false;

  public BadRequestException() {
    super(HTTP_STATUS);
  }

  public BadRequestException(Throwable cause) {
    super(HTTP_STATUS, cause);
  }

  public BadRequestException(String debugInfo) {
    super(HTTP_STATUS, debugInfo);
  }

  public BadRequestException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS, debugInfo, cause);
  }

  public BadRequestException(String error, String debugInfo) {
    super(HTTP_STATUS, error, debugInfo);
  }

  public BadRequestException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS, error, debugInfo, cause);
  }

  protected BadRequestException(
    String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace
  ) {
    super(HTTP_STATUS, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}