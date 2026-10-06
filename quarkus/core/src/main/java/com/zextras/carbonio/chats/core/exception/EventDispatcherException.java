// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serial;
import java.io.Serializable;

public class EventDispatcherException extends DependencyException implements Serializable {

  @Serial private static final long serialVersionUID = 8760142311571089095L;
  private static final DependencyType type = DependencyType.EVENT_DISPATCHER;

  public EventDispatcherException() {
    super(type);
  }

  public EventDispatcherException(Throwable cause) {
    super(type, cause);
  }

  public EventDispatcherException(String debugInfo) {
    super(type, debugInfo);
  }

  public EventDispatcherException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public EventDispatcherException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public EventDispatcherException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected EventDispatcherException(
      String error,
      String debugInfo,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}
