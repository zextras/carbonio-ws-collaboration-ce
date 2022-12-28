// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class ProfilingException extends DependencyException implements Serializable {

  private static final long           serialVersionUID = 6285897188507170501L;
  private static final DependencyType type             = DependencyType.PROFILING_SERVICE;

  public ProfilingException() {
    super(type);
  }

  public ProfilingException(Throwable cause) {
    super(type, cause);
  }

  public ProfilingException(String debugInfo) {
    super(type, debugInfo);
  }

  public ProfilingException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public ProfilingException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public ProfilingException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected ProfilingException(String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}