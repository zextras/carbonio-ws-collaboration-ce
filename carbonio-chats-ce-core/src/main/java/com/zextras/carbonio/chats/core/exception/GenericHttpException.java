// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import javax.ws.rs.core.Response.Status;

public class GenericHttpException extends ChatsHttpException {

  private boolean toLog = false;

  public GenericHttpException(int httpStatusCode, String httpStatusPhrase, String debugInfo) {
    super(httpStatusCode, httpStatusPhrase, debugInfo);
  }
  public GenericHttpException(int httpStatusCode, String httpStatusPhrase, String debugInfo, Throwable cause) {
    super(httpStatusCode, httpStatusPhrase, debugInfo, cause);
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
