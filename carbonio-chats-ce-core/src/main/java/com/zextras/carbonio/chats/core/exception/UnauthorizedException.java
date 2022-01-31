// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import java.io.Serializable;
import javax.ws.rs.core.Response.Status;

public class UnauthorizedException extends ChatsHttpException implements Serializable {

  private static final long    serialVersionUID = -1938248158031455490L;
  private static final Status  HTTP_STATUS      = Status.UNAUTHORIZED;
  private static final boolean IS_TO_LOG        = false;

  public UnauthorizedException() {
    super(HTTP_STATUS);
  }

  public UnauthorizedException(Throwable cause) {
    super(HTTP_STATUS, cause);
  }

  public UnauthorizedException(String debugInfo) {
    super(HTTP_STATUS, debugInfo);
  }

  public UnauthorizedException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS, debugInfo, cause);
  }

  public UnauthorizedException(String error, String debugInfo) {
    super(HTTP_STATUS, error, debugInfo);
  }

  public UnauthorizedException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS, error, debugInfo, cause);
  }

  protected UnauthorizedException(
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