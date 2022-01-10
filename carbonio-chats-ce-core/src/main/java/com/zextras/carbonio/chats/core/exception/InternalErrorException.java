package com.zextras.carbonio.chats.core.exception;


import java.io.Serializable;
import javax.ws.rs.core.Response.Status;

public class InternalErrorException extends ChatsHttpException implements Serializable {

  private static final long    serialVersionUID = -6182509366341339610L;
  private static final Status  HTTP_STATUS      = Status.INTERNAL_SERVER_ERROR;
  private static final boolean IS_TO_LOG        = true;

  public InternalErrorException() {
    super(HTTP_STATUS);
  }

  public InternalErrorException(Throwable cause) {
    super(HTTP_STATUS, cause);
  }

  public InternalErrorException(String debugInfo) {
    super(HTTP_STATUS, debugInfo);
  }

  public InternalErrorException(String debugInfo, Throwable cause) {
    super(HTTP_STATUS, debugInfo, cause);
  }

  public InternalErrorException(String error, String debugInfo) {
    super(HTTP_STATUS, error, debugInfo);
  }

  public InternalErrorException(String error, String debugInfo, Throwable cause) {
    super(HTTP_STATUS, error, debugInfo, cause);
  }

  protected InternalErrorException(
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