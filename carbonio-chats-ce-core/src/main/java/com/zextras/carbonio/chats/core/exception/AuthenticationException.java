// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class AuthenticationException extends DependencyException implements Serializable {

  private static final long           serialVersionUID = -4633091038679514501L;
  private static final DependencyType type             = DependencyType.AUTHENTICATION_SERVICE;

  public AuthenticationException() {
    super(type);
  }

  public AuthenticationException(Throwable cause) {
    super(type, cause);
  }

  public AuthenticationException(String debugInfo) {
    super(type, debugInfo);
  }

  public AuthenticationException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public AuthenticationException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public AuthenticationException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected AuthenticationException(String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}