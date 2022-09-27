// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class EventDispatcherException extends DependencyException implements Serializable {

  private static final long serialVersionUID = 8760142311571089095L;
  private static final boolean isRequired = DependencyType.EVENT_DISPATCHER.isRequired();

  public EventDispatcherException(DependencyType service) {
    super(service);
  }

  public EventDispatcherException(DependencyType service, Throwable cause) {
    super(service, cause);
  }

  public EventDispatcherException(DependencyType service, String debugInfo) {
    super(service, debugInfo);
  }

  public EventDispatcherException(DependencyType service, String debugInfo, Throwable cause) {
    super(service, debugInfo, cause);
  }

  public EventDispatcherException(DependencyType service, String error, String debugInfo) {
    super(service, error, debugInfo);
  }

  public EventDispatcherException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service, error, debugInfo, cause);
  }

  protected EventDispatcherException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return isRequired;
  }
}