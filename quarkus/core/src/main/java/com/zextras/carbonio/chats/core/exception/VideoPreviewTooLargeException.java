// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import jakarta.ws.rs.core.Response.Status;
import java.io.Serial;
import java.io.Serializable;

/**
 * Signals that a video preview is not produced because the source video exceeds the configured
 * {@code MAX_VIDEO_SIZE_PREVIEW_IN_MB} limit. The size gate never enqueues such videos, so no job
 * row ever exists for them. Maps to HTTP 413 Request Entity Too Large. Terminal from the client's
 * point of view (unless an operator raises the limit), so the client should stop polling.
 */
public class VideoPreviewTooLargeException extends ChatsHttpException implements Serializable {

  @Serial private static final long serialVersionUID = 7558391180461299123L;
  private static final Status HTTP_STATUS = Status.REQUEST_ENTITY_TOO_LARGE;
  private static final boolean IS_TO_LOG = false;

  public VideoPreviewTooLargeException() {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase());
  }

  public VideoPreviewTooLargeException(String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}
