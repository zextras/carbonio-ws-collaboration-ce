// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

/** {@code code} is the HTTP status, or 0 for a network failure/timeout. */
public class TurnServerException extends RuntimeException {
  private final int code;

  public TurnServerException(String message, int code, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
