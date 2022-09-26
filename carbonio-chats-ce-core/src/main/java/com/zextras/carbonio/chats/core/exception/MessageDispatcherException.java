// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class MessageDispatcherException extends DependencyException implements Serializable {

  private static final long serialVersionUID = 2549718943520474692L;
  private static final boolean isRequired = DependencyType.XMPP_SERVER.isRequired();
  private static final boolean IS_TO_LOG  = false;

  public MessageDispatcherException() {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE);
  }

  public MessageDispatcherException(Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, cause);
  }

  public MessageDispatcherException(String debugInfo) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo);
  }

  public MessageDispatcherException(String debugInfo, Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo, cause);
  }

  public MessageDispatcherException(String error, String debugInfo) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo);
  }

  public MessageDispatcherException(String error, String debugInfo, Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo, cause);
  }

  protected MessageDispatcherException(
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