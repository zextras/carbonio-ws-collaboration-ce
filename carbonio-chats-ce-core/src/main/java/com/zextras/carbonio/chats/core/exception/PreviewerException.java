// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;


import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class PreviewerException extends DependencyException implements Serializable {

  private static final long           serialVersionUID = 5335428065127434966L;
  private static final DependencyType type             = DependencyType.PREVIEWER_SERVICE;

  public PreviewerException() {
    super(type);
  }

  public PreviewerException(Throwable cause) {
    super(type, cause);
  }

  public PreviewerException(String debugInfo) {
    super(type, debugInfo);
  }

  public PreviewerException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public PreviewerException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public PreviewerException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected PreviewerException(String error, String debugInfo, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}