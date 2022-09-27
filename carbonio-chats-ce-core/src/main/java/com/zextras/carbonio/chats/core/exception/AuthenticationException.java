// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class AuthenticationException extends DependencyException implements Serializable {

  private static final long    serialVersionUID = -4633091038679514501L;
  private static final boolean isRequired       = DependencyType.AUTHENTICATION_SERVICE.isRequired();

  public AuthenticationException(DependencyType service) {
    super(service);
  }

  public AuthenticationException(DependencyType service, Throwable cause) {
    super(service, cause);
  }

  public AuthenticationException(DependencyType service, String debugInfo) {
    super(service, debugInfo);
  }

  public AuthenticationException(DependencyType service, String debugInfo, Throwable cause) {
    super(service, debugInfo, cause);
  }

  public AuthenticationException(DependencyType service, String error, String debugInfo) {
    super(service, error, debugInfo);
  }

  public AuthenticationException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service, error, debugInfo, cause);
  }

  protected AuthenticationException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return isRequired;
  }
}