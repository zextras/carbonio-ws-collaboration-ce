// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.exception;

import jakarta.ws.rs.core.Response.Status;
import java.io.Serial;
import java.io.Serializable;

/**
 * Signals that a video preview cannot be produced because the source codec/format is not decodable
 * (e.g. AV1 or a corrupt stream). Maps to HTTP 415 Unsupported Media Type. Terminal: the persisted
 * job is UNSUPPORTED and is never re-attempted, so the client should stop polling and surface a
 * "format not supported" state.
 */
public class VideoPreviewUnsupportedException extends ChatsHttpException implements Serializable {

  @Serial private static final long serialVersionUID = 1672455990772631742L;
  private static final Status HTTP_STATUS = Status.UNSUPPORTED_MEDIA_TYPE;
  private static final boolean IS_TO_LOG = false;

  public VideoPreviewUnsupportedException() {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase());
  }

  public VideoPreviewUnsupportedException(String debugInfo) {
    super(HTTP_STATUS.getStatusCode(), HTTP_STATUS.getReasonPhrase(), debugInfo);
  }

  @Override
  public boolean isToLog() {
    return IS_TO_LOG;
  }
}
