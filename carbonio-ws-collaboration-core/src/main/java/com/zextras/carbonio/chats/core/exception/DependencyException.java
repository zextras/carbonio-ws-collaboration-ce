// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import jakarta.ws.rs.core.Response.Status;
import java.io.Serial;
import org.jetbrains.annotations.NotNull;

/**
 * This is an abstract class implemented by dependent services exceptions that wrap an HTTP error
 * response. Each class that extends this represents a specific dependent service.
 */
public abstract class DependencyException extends ChatsHttpException {

  @Serial private static final long serialVersionUID = -8436905681137000221L;
  private static final int MANDATORY_HTTP_STATUS_CODE =
      Status.INTERNAL_SERVER_ERROR.getStatusCode();
  private static final String MANDATORY_HTTP_REASON_PHRASE =
      Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
  private static final int OPTIONAL_HTTP_STATUS_CODE = 424;
  private static final String OPTIONAL_HTTP_REASON_PHRASE = "Failed dependency";
  private final DependencyType type;

  public DependencyException(DependencyType type) {
    super(getHttpStatusCode(type), getHttpStatusPhrase(type));
    this.type = type;
  }

  public DependencyException(DependencyType type, Throwable cause) {
    super(getHttpStatusCode(type), getHttpStatusPhrase(type), cause);
    this.type = type;
  }

  public DependencyException(DependencyType type, String debugInfo) {
    super(getHttpStatusCode(type), getHttpStatusPhrase(type), debugInfo);
    this.type = type;
  }

  public DependencyException(DependencyType type, String debugInfo, Throwable cause) {
    super(getHttpStatusCode(type), getHttpStatusPhrase(type), debugInfo, cause);
    this.type = type;
  }

  public DependencyException(DependencyType type, String error, String debugInfo) {
    super(getHttpStatusCode(type), getHttpStatusPhrase(type), error, debugInfo);
    this.type = type;
  }

  public DependencyException(DependencyType type, String error, String debugInfo, Throwable cause) {
    super(getHttpStatusCode(type), getHttpStatusPhrase(type), error, debugInfo, cause);
    this.type = type;
  }

  protected DependencyException(
      DependencyType type,
      String error,
      String debugInfo,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(
        getHttpStatusCode(type),
        getHttpStatusPhrase(type),
        error,
        debugInfo,
        cause,
        enableSuppression,
        writableStackTrace);
    this.type = type;
  }

  private static int getHttpStatusCode(@NotNull DependencyType type) {
    return type.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE;
  }

  private static String getHttpStatusPhrase(@NotNull DependencyType type) {
    return type.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE;
  }

  public DependencyType getType() {
    return type;
  }

  public boolean isToLog() {
    return true;
  }
}
