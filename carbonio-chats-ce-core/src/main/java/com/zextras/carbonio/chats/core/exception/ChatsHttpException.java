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

  private final Status httpStatus;
  private final String error;
  private final String debugInfo;

  public ChatsHttpException(Status httpStatus) {
    super(String.join(" - ", httpStatus.getReasonPhrase(), httpStatus.getReasonPhrase()));
    this.httpStatus = httpStatus;
    this.error = httpStatus.getReasonPhrase();
    this.debugInfo = httpStatus.getReasonPhrase();
  }

  public ChatsHttpException(Status httpStatus, Throwable cause) {
    super(String.join(" - ", httpStatus.getReasonPhrase(), httpStatus.getReasonPhrase()), cause);
    this.httpStatus = httpStatus;
    this.error = httpStatus.getReasonPhrase();
    this.debugInfo = httpStatus.getReasonPhrase();
  }

  public ChatsHttpException(Status httpStatus, String debugInfo) {
    super(String.join(" - ", httpStatus.getReasonPhrase(), debugInfo));
    this.httpStatus = httpStatus;
    this.error = httpStatus.getReasonPhrase();
    this.debugInfo = debugInfo;
  }

  public ChatsHttpException(Status httpStatus, String debugInfo, Throwable cause) {
    super(String.join(" - ", httpStatus.getReasonPhrase(), debugInfo), cause);
    this.httpStatus = httpStatus;
    this.error = httpStatus.getReasonPhrase();
    this.debugInfo = debugInfo;
  }

  public ChatsHttpException(Status httpStatus, String error, String debugInfo) {
    super(String.join(" - ", error, debugInfo));
    this.httpStatus = httpStatus;
    this.error = error;
    this.debugInfo = debugInfo;
  }

  public ChatsHttpException(Status httpStatus, String error, String debugInfo, Throwable cause) {
    super(String.join(" - ", error, debugInfo), cause);
    this.httpStatus = httpStatus;
    this.error = error;
    this.debugInfo = debugInfo;
  }

  protected ChatsHttpException(
    Status httpStatus, String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace
  ) {
    super(String.join(" - ", error, debugInfo), cause, enableSuppression, writableStackTrace);
    this.httpStatus = httpStatus;
    this.error = error;
    this.debugInfo = debugInfo;
  }

  public Status getHttpStatus() {
    return httpStatus;
  }

  public String getError() {
    return error;
  }

  public String getDebugInfo() {
    return debugInfo;
  }

  public abstract boolean isToLog();
}
