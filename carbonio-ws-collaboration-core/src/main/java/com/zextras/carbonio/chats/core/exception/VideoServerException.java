// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import java.io.Serial;
import java.io.Serializable;

public class VideoServerException extends DependencyException implements Serializable {

  @Serial private static final long serialVersionUID = -5253995448373386702L;
  private static final DependencyType type = DependencyType.VIDEOSERVER_SERVICE;

  public VideoServerException() {
    super(type);
  }

  public VideoServerException(Throwable cause) {
    super(type, cause);
  }

  public VideoServerException(String debugInfo) {
    super(type, debugInfo);
  }

  public VideoServerException(String debugInfo, Throwable cause) {
    super(type, debugInfo, cause);
  }

  public VideoServerException(String error, String debugInfo) {
    super(type, error, debugInfo);
  }

  public VideoServerException(String error, String debugInfo, Throwable cause) {
    super(type, error, debugInfo, cause);
  }

  protected VideoServerException(
      String error,
      String debugInfo,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(type, error, debugInfo, cause, enableSuppression, writableStackTrace);
  }
}
