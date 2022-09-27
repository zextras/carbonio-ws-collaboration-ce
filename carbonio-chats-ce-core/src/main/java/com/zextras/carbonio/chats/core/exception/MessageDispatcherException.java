// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class MessageDispatcherException extends DependencyException implements Serializable {

  private static final long serialVersionUID = 2549718943520474692L;

  private static final boolean isRequired = DependencyType.XMPP_SERVER.isRequired();

  public MessageDispatcherException(DependencyType service) {
    super(service);
  }

  public MessageDispatcherException(DependencyType service, Throwable cause) {
    super(service, cause);
  }

  public MessageDispatcherException(DependencyType service, String debugInfo) {
    super(service, debugInfo);
  }

  public MessageDispatcherException(DependencyType service, String debugInfo, Throwable cause) {
    super(service, debugInfo, cause);
  }

  public MessageDispatcherException(DependencyType service, String error, String debugInfo) {
    super(service, error, debugInfo);
  }

  public MessageDispatcherException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service, error, debugInfo, cause);
  }

  protected MessageDispatcherException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return isRequired;
  }
}