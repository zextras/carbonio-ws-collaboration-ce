// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class ProfilingException extends DependencyException implements Serializable {

  private static final long    serialVersionUID = 6285897188507170501L;
  private static final boolean isRequired       = DependencyType.PROFILING_SERVICE.isRequired();

  public ProfilingException(DependencyType service) {
    super(service);
  }

  public ProfilingException(DependencyType service, Throwable cause) {
    super(service, cause);
  }

  public ProfilingException(DependencyType service, String debugInfo) {
    super(service, debugInfo);
  }

  public ProfilingException(DependencyType service, String debugInfo, Throwable cause) {
    super(service, debugInfo, cause);
  }

  public ProfilingException(DependencyType service, String error, String debugInfo) {
    super(service, error, debugInfo);
  }

  public ProfilingException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service, error, debugInfo, cause);
  }

  protected ProfilingException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return isRequired;
  }
}