// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class ProfilingException extends DependencyException implements Serializable {

  private static final long serialVersionUID = 6285897188507170501L;
  private static final boolean isRequired = DependencyType.PROFILING_SERVICE.isRequired();
  private static final boolean IS_TO_LOG        = false;

  public ProfilingException() {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE);
  }

  public ProfilingException(Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, cause);
  }

  public ProfilingException(String debugInfo) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo);
  }

  public ProfilingException(String debugInfo, Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, debugInfo, cause);
  }

  public ProfilingException(String error, String debugInfo) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo);
  }

  public ProfilingException(String error, String debugInfo, Throwable cause) {
    super(isRequired ? MANDATORY_HTTP_STATUS_CODE : OPTIONAL_HTTP_STATUS_CODE,
      isRequired ? MANDATORY_HTTP_REASON_PHRASE : OPTIONAL_HTTP_REASON_PHRASE, error, debugInfo, cause);
  }

  protected ProfilingException(
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