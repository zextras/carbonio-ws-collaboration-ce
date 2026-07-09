// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import java.io.Serial;
import java.io.Serializable;

/**
 * Signals that video preview generation was attempted and definitively gave up: the source blob is
 * gone, or the bounded soft-failure cap was reached. Maps to HTTP 422 Unprocessable Entity.
 * Terminal: the persisted job is FAILED and is never re-attempted, so the client should stop
 * polling and surface a "preview unavailable" state.
 *
 * <p>422 has no constant in the {@code jakarta.ws.rs} {@code Response.Status} enum (it was never
 * added to JAX-RS), so the status code is declared as a raw int with its standard reason phrase —
 * the same convention already used elsewhere in this codebase (see {@code VersionedRequestFilter}).
 */
public class VideoPreviewFailedException extends ChatsHttpException implements Serializable {

  @Serial private static final long serialVersionUID = 5128330664421775912L;
  private static final int HTTP_STATUS_CODE = 422;
  private static final String HTTP_STATUS_PHRASE = "Unprocessable Entity";
  private static final boolean IS_TO_LOG = false;

  public VideoPreviewFailedException() {
    super(HTTP_STATUS_CODE, HTTP_STATUS_PHRASE);
  }

  public VideoPreviewFailedException(String debugInfo) {
    super(HTTP_STATUS_CODE, HTTP_STATUS_PHRASE, debugInfo);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}
