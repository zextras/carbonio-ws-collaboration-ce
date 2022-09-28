// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class MessageDispatcherException extends DependencyException implements Serializable {

  private static final long serialVersionUID = 2549718943520474692L;

  private static final DependencyType type = DependencyType.XMPP_SERVER;

  public MessageDispatcherException() {
    super(type);
  }

  public MessageDispatcherException(Throwable cause) {
    super(type, cause);
  }

  public MessageDispatcherException(String debugInfo) {
    super(type, debugInfo);
  }

  public MessageDispatcherException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public MessageDispatcherException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public MessageDispatcherException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected MessageDispatcherException(String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}