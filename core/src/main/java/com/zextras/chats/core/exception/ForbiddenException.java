package com.zextras.chats.core.exception;

import java.io.Serializable;
import javax.ws.rs.core.Response.Status;

public class ForbiddenException extends ChatsHttpException implements Serializable {

  private static final long    serialVersionUID = -115828843775102406L;
  private static final Status  HTTP_STATUS      = Status.FORBIDDEN;
  private static final boolean IS_TO_LOG        = false;

  public ForbiddenException() {
    super(HTTP_STATUS);
  }

  public ForbiddenException(Throwable cause) {
    super(HTTP_STATUS, cause);
  }

  public ForbiddenException(String debugInfo) {
    super(HTTP_STATUS, debugInfo);
  }

  public ForbiddenException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS, debugInfo, cause);
  }

  public ForbiddenException(String error, String debugInfo) {
    super(HTTP_STATUS, error, debugInfo);
  }

  public ForbiddenException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS, error, debugInfo, cause);
  }

  protected ForbiddenException(
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