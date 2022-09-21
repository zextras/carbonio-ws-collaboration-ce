// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import javax.ws.rs.core.Response.Status;

/**
 * This is an abstract class implemented by Chats exceptions that wrap an http error response. Each class that extends
 * this represents a specific http status.
 */
public abstract class ChatsHttpException extends RuntimeException {

  private static final long serialVersionUID = 5522054402537696862L;

  private final int    httpStatusCode;
  private final String httpStatusPhrase;
  private final String error;
  private final String debugInfo;

  public ChatsHttpException(int httpStatusCode, String httpStatusPhrase) {
    super(String.join(" - ", httpStatusPhrase, httpStatusPhrase));
    this.httpStatusCode = httpStatusCode;
    this.httpStatusPhrase = httpStatusPhrase;
    this.error = httpStatusPhrase;
    this.debugInfo = httpStatusPhrase;
  }

  public ChatsHttpException(int httpStatusCode, String httpStatusPhrase, Throwable cause) {
    super(String.join(" - ", httpStatusPhrase, httpStatusPhrase), cause);
    this.httpStatusCode = httpStatusCode;
    this.httpStatusPhrase = httpStatusPhrase;
    this.error = httpStatusPhrase;
    this.debugInfo = httpStatusPhrase;
  }

  public ChatsHttpException(int httpStatusCode, String httpStatusPhrase, String debugInfo) {
    super(String.join(" - ", httpStatusPhrase, debugInfo));
    this.httpStatusCode = httpStatusCode;
    this.httpStatusPhrase = httpStatusPhrase;
    this.error = httpStatusPhrase;
    this.debugInfo = debugInfo;
  }

  public ChatsHttpException(int httpStatusCode, String httpStatusPhrase, String debugInfo, Throwable cause) {
    super(String.join(" - ", httpStatusPhrase, debugInfo), cause);
    this.httpStatusCode = httpStatusCode;
    this.httpStatusPhrase = httpStatusPhrase;
    this.error = httpStatusPhrase;
    this.debugInfo = debugInfo;
  }

  public ChatsHttpException(int httpStatusCode, String httpStatusPhrase, String error, String debugInfo) {
    super(String.join(" - ", error, debugInfo));
    this.httpStatusCode = httpStatusCode;
    this.httpStatusPhrase = httpStatusPhrase;
    this.error = error;
    this.debugInfo = debugInfo;
  }

  public ChatsHttpException(int httpStatusCode, String httpStatusPhrase, String error, String debugInfo,
    Throwable cause) {
    super(String.join(" - ", error, debugInfo), cause);
    this.httpStatusCode = httpStatusCode;
    this.httpStatusPhrase = httpStatusPhrase;
    this.error = error;
    this.debugInfo = debugInfo;
  }

  protected ChatsHttpException(
    int httpStatusCode, String httpStatusPhrase, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace
  ) {
    super(String.join(" - ", error, debugInfo), cause, enableSuppression, writableStackTrace);
    this.httpStatusCode = httpStatusCode;
    this.httpStatusPhrase = httpStatusPhrase;
    this.error = error;
    this.debugInfo = debugInfo;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public String getHttpStatusPhrase() {
    return httpStatusPhrase;
  }

  public String getError() {
    return error;
  }

  public String getDebugInfo() {
    return debugInfo;
  }

  public abstract boolean isToLog();
}
