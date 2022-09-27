// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class PreviewerException extends DependencyException implements Serializable {

  private static final long    serialVersionUID = 5335428065127434966L;
  private static final boolean isRequired       = DependencyType.PREVIEWER_SERVICE.isRequired();

  public PreviewerException(DependencyType service) {
    super(service);
  }

  public PreviewerException(DependencyType service, Throwable cause) {
    super(service, cause);
  }

  public PreviewerException(DependencyType service, String debugInfo) {
    super(service, debugInfo);
  }

  public PreviewerException(DependencyType service, String debugInfo, Throwable cause) {
    super(service, debugInfo, cause);
  }

  public PreviewerException(DependencyType service, String error, String debugInfo) {
    super(service, error, debugInfo);
  }

  public PreviewerException(DependencyType service, String error, String debugInfo, Throwable cause) {
    super(service, error, debugInfo, cause);
  }

  protected PreviewerException(DependencyType service, String error, String debugInfo, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace) {
    super(service, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public boolean isToLog() {
    return isRequired;
  }
}