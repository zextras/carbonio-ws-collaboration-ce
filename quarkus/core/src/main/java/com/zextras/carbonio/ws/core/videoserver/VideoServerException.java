// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.videoserver;

public class VideoServerException extends RuntimeException {
  public VideoServerException(String message) {
    super(message);
  }

  public VideoServerException(String message, Throwable cause) {
    super(message, cause);
  }
}
