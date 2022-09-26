// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class AuthenticationException extends DependencyException implements Serializable {

  private static final long serialVersionUID = -4633091038679514501L;
  private static final boolean isRequired = DependencyType.AUTHENTICATION_SERVICE.isRequired();
  private static final boolean IS_TO_LOG  = false;

  public AuthenticationException() {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE);
  }

  public AuthenticationException(Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, cause);
  }

  public AuthenticationException(String debugInfo) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo);
  }

  public AuthenticationException(String debugInfo, Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo, cause);
  }

  public AuthenticationException(String error, String debugInfo) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo);
  }

  public AuthenticationException(String error, String debugInfo, Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo, cause);
  }

  protected AuthenticationException(
    String error, String debugInfo, Throwable cause, boolean enableSuppression, boolean writableStackTrace
  ) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo, cause,
      enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }

  @Override
  public int getStatusCode() {
    return isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE;
  }

  @Override
  public String getReasonPhrase() {
    return isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE;
  }
}