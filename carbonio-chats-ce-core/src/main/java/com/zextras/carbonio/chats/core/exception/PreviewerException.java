// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import java.io.Serializable;
import javax.ws.rs.core.Response.Status;

public class PreviewerException extends ChatsHttpException implements Serializable {

  private static final long serialVersionUID = 5335428065127434966L;
  private static final Status  HTTP_STATUS      = Status.TOO_MANY_REQUESTS;
  private static final boolean IS_TO_LOG        = false;

  public PreviewerException() {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase());
  }

  public PreviewerException(Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), cause);
  }

  public PreviewerException(String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo);
  }

  public PreviewerException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo, cause);
  }

  public PreviewerException(String error, String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo);
  }

  public PreviewerException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo, cause);
  }

  protected PreviewerException(
    String error, String debugInfo, Throwable cause, boolean enableSuppression, boolean writableStackTrace
  ) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), error, debugInfo, cause, enableSuppression,
      writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}