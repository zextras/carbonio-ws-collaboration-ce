// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class StorageException extends DependencyException implements Serializable {

  private static final long           serialVersionUID = -1921616994011018008L;
  private static final DependencyType type             = DependencyType.STORAGE_SERVICE;

  public StorageException() {
    super(type);
  }

  public StorageException(Throwable cause) {
    super(type, cause);
  }

  public StorageException(String debugInfo) {
    super(type, debugInfo);
  }

  public StorageException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public StorageException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public StorageException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected StorageException(String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}