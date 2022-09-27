// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class DatabaseException extends DependencyException implements Serializable {

  private static final long serialVersionUID = 6018988481675399724L;
  private static final boolean isRequired = DependencyType.DATABASE.isRequired();

  public DatabaseException(DependencyType service) {
    super(service);
  }

  public DatabaseException(DependencyType service, Throwable cause) {
    super(service, cause);
  }

  public DatabaseException(DependencyType service, String debugInfo) {
    super(service, debugInfo);
  }

  public DatabaseException(DependencyType service, String debugInfo, Throwable cause) {
    super(service, debugInfo, cause);
  }

  public DatabaseException(DependencyType service, String error, String debugInfo) {
    super(service, error, debugInfo);
  }

  public DatabaseException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service, error, debugInfo, cause);
  }

  protected DatabaseException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return isRequired;
  }
}