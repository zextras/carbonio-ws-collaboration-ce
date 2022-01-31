// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import java.io.Serializable;
import javax.ws.rs.core.Response.Status;

public class GatewayTimeoutException extends ChatsHttpException implements Serializable {

  private static final long    serialVersionUID = 2119222848452440652L;
  private static final Status  HTTP_STATUS      = Status.GATEWAY_TIMEOUT;
  private static final boolean IS_TO_LOG        = false;


  public GatewayTimeoutException() {
    super(HTTP_STATUS);
  }

  public GatewayTimeoutException(Throwable cause) {
    super(HTTP_STATUS, cause);
  }

  public GatewayTimeoutException(String debugInfo) {
    super(HTTP_STATUS, debugInfo);
  }

  public GatewayTimeoutException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS, debugInfo, cause);
  }

  public GatewayTimeoutException(String error, String debugInfo) {
    super(HTTP_STATUS, error, debugInfo);
  }

  public GatewayTimeoutException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS, error, debugInfo, cause);
  }

  protected GatewayTimeoutException(
    String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace
  ) {
    super(HTTP_STATUS, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}