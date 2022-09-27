// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class StorageException extends DependencyException implements Serializable {

  private static final long           serialVersionUID = -1921616994011018008L;
  private static final boolean        isRequired       = DependencyType.STORAGE_SERVICE.isRequired();

  public StorageException(DependencyType service) {
    super(service);
  }

  public StorageException(DependencyType service, Throwable cause) {
    super(service, cause);
  }

  public StorageException(DependencyType service, String debugInfo) {
    super(service, debugInfo);
  }

  public StorageException(DependencyType service, String debugInfo, Throwable cause) {
    super(service, debugInfo, cause);
  }

  public StorageException(DependencyType service, String error, String debugInfo) {
    super(service, error, debugInfo);
  }

  public StorageException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service, error, debugInfo, cause);
  }

  protected StorageException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return isRequired;
  }
}