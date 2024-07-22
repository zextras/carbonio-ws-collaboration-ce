// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Singleton
public class ChatsHttpExceptionHandler extends ExceptionHandler<ChatsHttpException> {

  @Inject
  public ChatsHttpExceptionHandler() {}

  @Override
  public Response toResponse(ChatsHttpException exception) {
    return handleException(
        exception,
        exception.getDebugInfo(),
        exception.getHttpStatusCode(),
        exception.getHttpStatusPhrase(),
        exception.isToLog());
  }
}
