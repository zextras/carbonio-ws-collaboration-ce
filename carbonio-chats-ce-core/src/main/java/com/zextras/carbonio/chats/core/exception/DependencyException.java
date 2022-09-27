// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import javax.ws.rs.core.Response.Status;

/**
 * This is an abstract class implemented by dependent services exceptions that wrap an HTTP error response. Each class
 * that extends this represents a specific dependent service.
 */
public abstract class DependencyException extends ChatsHttpException {

  private static final long serialVersionUID = -8436905681137000221L;

  protected static final int    MANDATORY_HTTP_STATUS_CODE   = Status.INTERNAL_SERVER_ERROR.getStatusCode();
  protected static final String MANDATORY_HTTP_REASON_PHRASE = Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
  protected static final int    OPTIONAL_HTTP_STATUS_CODE    = 424;
  protected static final String OPTIONAL_HTTP_REASON_PHRASE  = "Failed dependency";

  public DependencyException(DependencyType service) {
    super(service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE);
  }

  public DependencyException(DependencyType service ,Throwable cause) {
    super(service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, cause);
  }

  public DependencyException(DependencyType service, String debugInfo) {
    super(service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo);
  }

  public DependencyException(DependencyType service, String debugInfo, Throwable cause) {
    super(service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo, cause);
  }

  public DependencyException(DependencyType service, String error, String debugInfo) {
    super(service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo);
  }

  public DependencyException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo, cause);
  }

  protected DependencyException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo, cause,
      enableSuppression, writableStackTrace);
  }

  public int getHttpStatusCode(DependencyType service) {
    return service.isRequired() ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE;
  }

  public String getHttpStatusPhrase(DependencyType service) {
    return service.isRequired() ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE;
  }
}
