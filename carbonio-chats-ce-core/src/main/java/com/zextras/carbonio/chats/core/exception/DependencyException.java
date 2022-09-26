// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import javax.ws.rs.core.Response.Status;

/**
 * This is an abstract class implemented by dependent services exceptions that wrap an HTTP error response. Each class
 * that extends this represents a specific dependent service.
 */
public abstract class DependencyException extends RuntimeException {

  private static final   long   serialVersionUID   = -8436905681137000221L;
  protected static final int    MANDATORY_HTTP_STATUS_CODE   = Status.TOO_MANY_REQUESTS.getStatusCode();
  protected static final String MANDATORY_HTTP_REASON_PHRASE = Status.TOO_MANY_REQUESTS.getReasonPhrase();
  protected static final int    OPTIONAL_HTTP_STATUS_CODE   = Status.INTERNAL_SERVER_ERROR.getStatusCode();
  protected static final String OPTIONAL_HTTP_REASON_PHRASE = Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
  private final          String error;
  private final          String debugInfo;

  public DependencyException(int HTTP_STATUS_CODE, String HTTP_REASON_PHRASE) {
    super(String.join(" - ", HTTP_REASON_PHRASE, HTTP_REASON_PHRASE));
    this.error = HTTP_REASON_PHRASE;
    this.debugInfo = HTTP_REASON_PHRASE;
  }

  public DependencyException(int HTTP_STATUS_CODE, String HTTP_REASON_PHRASE, Throwable cause) {
    super(String.join(" - ", HTTP_REASON_PHRASE, HTTP_REASON_PHRASE), cause);
    this.error = HTTP_REASON_PHRASE;
    this.debugInfo = HTTP_REASON_PHRASE;
  }

  public DependencyException(int HTTP_STATUS_CODE, String HTTP_REASON_PHRASE, String debugInfo) {
    super(String.join(" - ", HTTP_REASON_PHRASE, debugInfo));
    this.error = HTTP_REASON_PHRASE;
    this.debugInfo = debugInfo;
  }

  public DependencyException(int HTTP_STATUS_CODE, String HTTP_REASON_PHRASE, String debugInfo, Throwable cause) {
    super(String.join(" - ", HTTP_REASON_PHRASE, debugInfo), cause);
    this.error = HTTP_REASON_PHRASE;
    this.debugInfo = debugInfo;
  }

  public DependencyException(int HTTP_STATUS_CODE, String HTTP_REASON_PHRASE, String error, String debugInfo) {
    super(String.join(" - ", error, debugInfo));
    this.error = error;
    this.debugInfo = debugInfo;
  }

  public DependencyException(int HTTP_STATUS_CODE, String HTTP_REASON_PHRASE, String error, String debugInfo,
    Throwable cause) {
    super(String.join(" - ", error, debugInfo), cause);
    this.error = error;
    this.debugInfo = debugInfo;
  }

  protected DependencyException(
    int HTTP_STATUS_CODE, String HTTP_REASON_PHRASE, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace
  ) {
    super(String.join(" - ", error, debugInfo), cause, enableSuppression, writableStackTrace);
    this.error = error;
    this.debugInfo = debugInfo;
  }

  public abstract int getStatusCode();

  public abstract String getReasonPhrase();

  public String getError() {
    return error;
  }

  public String getDebugInfo() {
    return debugInfo;
  }

  public abstract boolean isToLog();
}
