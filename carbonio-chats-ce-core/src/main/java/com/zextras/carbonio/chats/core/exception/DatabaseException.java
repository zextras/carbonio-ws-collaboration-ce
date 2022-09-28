// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class DatabaseException extends DependencyException implements Serializable {

  private static final long           serialVersionUID = 6018988481675399724L;
  private static final DependencyType type             = DependencyType.DATABASE;

  public DatabaseException() {
    super(type);
  }

  public DatabaseException(Throwable cause) {
    super(type, cause);
  }

  public DatabaseException(String debugInfo) {
    super(type, debugInfo);
  }

  public DatabaseException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public DatabaseException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public DatabaseException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected DatabaseException(String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}