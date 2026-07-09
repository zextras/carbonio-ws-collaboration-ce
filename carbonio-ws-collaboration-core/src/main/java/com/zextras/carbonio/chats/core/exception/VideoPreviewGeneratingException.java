// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import jakarta.ws.rs.core.Response.Status;
import java.io.Serial;
import java.io.Serializable;

/**
 * Signals that a video preview is still being generated (the persisted job is PENDING or
 * GENERATING, or a lazy generation was just triggered). Maps to HTTP 202 Accepted: the request was
 * accepted but the frame is not ready yet, so the client should retry later. Non-terminal — unlike
 * {@link VideoPreviewUnsupportedException} / {@link VideoPreviewFailedException} the client should
 * keep polling.
 */
public class VideoPreviewGeneratingException extends ChatsHttpException implements Serializable {

  @Serial private static final long serialVersionUID = 4061286994912271871L;
  private static final Status HTTP_STATUS = Status.ACCEPTED;
  private static final boolean IS_TO_LOG = false;

  public VideoPreviewGeneratingException() {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase());
  }

  public VideoPreviewGeneratingException(String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}
