// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import javax.ws.rs.core.Response.Status;

public class GenericHttpException extends ChatsHttpException {

  private boolean toLog = false;

  public GenericHttpException(int httpStatus, String debugInfo) {
    super(Status.fromStatusCode(httpStatus), debugInfo);
  }
  public GenericHttpException(int httpStatus, String debugInfo, Throwable cause) {
    super(Status.fromStatusCode(httpStatus), debugInfo, cause);
  }

  @Override
  public boolean isToLog() {
    return toLog;
  }

  public GenericHttpException toLog(boolean toLog) {
    this.toLog = toLog;
    return this;
  }
}
