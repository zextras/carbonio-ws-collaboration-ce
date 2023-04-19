// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serializable;

public class PreviewException extends DependencyException implements Serializable {

  private static final long           serialVersionUID = 5335428065127434966L;
  private static final DependencyType type             = DependencyType.PREVIEWER_SERVICE;

  public PreviewException() {
    super(type);
  }

  public PreviewException(Throwable cause) {
    super(type, cause);
  }

  public PreviewException(String debugInfo) {
    super(type, debugInfo);
  }

  public PreviewException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public PreviewException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public PreviewException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected PreviewException(String error, String debugInfo, Throwable cause, boolean enableSuppression,
                             boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}