// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import java.io.Serializable;
import javax.ws.rs.core.Response.Status;

public class NotFoundException extends ChatsHttpException implements Serializable {

  private static final long    serialVersionUID = -7586350573263929718L;
  private static final Status  HTTP_STATUS      = Status.NOT_FOUND;
  private static final boolean IS_TO_LOG        = true;

  public NotFoundException() {
    super(HTTP_STATUS);
  }

  public NotFoundException(Throwable cause) {
    super(HTTP_STATUS, cause);
  }

  public NotFoundException(String debugInfo) {
    super(HTTP_STATUS, debugInfo);
  }

  public NotFoundException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS, debugInfo, cause);
  }

  public NotFoundException(String error, String debugInfo) {
    super(HTTP_STATUS, error, debugInfo);
  }

  public NotFoundException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS, error, debugInfo, cause);
  }

  protected NotFoundException(
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